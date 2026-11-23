package dev.nyon.magnetic

import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.centerVec
import dev.nyon.magnetic.mixins.ExperienceOrbInvoker
import dev.nyon.magnetic.utils.CooldownTracker
import dev.nyon.magnetic.utils.MixinHelper
import dev.nyon.magnetic.utils.MixinHelper.conditionAlreadyChecked
import dev.nyon.magnetic.utils.ThreadLocalScope
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.mutable.MutableInt
import java.util.UUID

object DropEvent {
    @Suppress("KotlinConstantConditions")
    operator fun invoke(
        items: MutableList<ItemStack>,
        exp: MutableInt,
        player: ServerPlayer,
        pos: BlockPos
    ) {
        if (conditionAlreadyChecked.get() != true && !config.conditionStatement.checkAndReport(player)) return

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (config.animation.enabled) {
                    val canAddItem = try {
                        canAddItem(item, player)
                    } catch (_: RuntimeException) {
                        return@removeIf false
                    }
                    if (canAddItem) {
                        Animation.pullItemToPlayer(item, pos.centerVec(), player)
                        return@removeIf true
                    }
                }

                if (item.isEmpty) return@removeIf true
                val remaining = pickUp(item, player)
                if (remaining.isEmpty) return@removeIf true
                item.count = remaining.count
                if (isInventoryFull(remaining, player)) tickInventoryAlert(player)
                false
            }
        }

        if (config.expAllowed) {
            val fakeExperienceOrb = ExperienceOrb(player.level(), 0.0, 0.0, 0.0, exp.toInt())
            player.take(fakeExperienceOrb, 1)
            val leftExp = (fakeExperienceOrb as ExperienceOrbInvoker)
                .invokeRepairPlayerItems(player, exp.toInt())
            if (leftExp > 0) player.giveExperiencePoints(leftExp)
            exp.value = 0
        }
    }

    private val cooldowns = CooldownTracker<Pair<AlertType, UUID>>()

    private fun tickInventoryAlert(player: ServerPlayer) {
        val alerts = config.fullInventoryAlert.let {
            listOf(
                AlertType.SOUND to it.soundAlert,
                AlertType.TEXT to it.textAlert,
                AlertType.TITLE to it.titleAlert
            )
        }
        alerts.forEach { (type, alert) ->
            if (!alert.enabled) return@forEach
            if (cooldowns.tryAcquire(type to player.uuid, alert.cooldownInSeconds * 1_000L)) {
                alert.invoke(player)
            }
        }
    }

    /**
     * Hands [stack] to [player] through vanilla's item pickup instead of inserting it directly, so mods that
     * hook pickup see Magnetic's drops: backpack pickup upgrades, pickup filters, and NeoForge's
     * `ItemEntityPickupEvent`. The transient entity is never added to the level.
     *
     * @return the part of [stack] the player did not take; empty when everything was taken or a mod consumed it
     */
    private fun pickUp(stack: ItemStack, player: ServerPlayer): ItemStack {
        val itemEntity = ItemEntity(player.level(), player.x, player.y, player.z, stack.copy())
        itemEntity.setNoPickUpDelay()
        itemEntity.setTarget(player.uuid)
        val inserted = MutableInt()
        ThreadLocalScope.run(MixinHelper.directPickup, inserted) { itemEntity.playerTouch(player) }
        // ItemEntityMixin measures what reached the inventory; items taken by other mods are theirs to count.
        if (inserted.toInt() > 0) player.awardStat(Stats.ITEM_PICKED_UP.get(stack.item), inserted.toInt())
        return if (itemEntity.isRemoved) ItemStack.EMPTY else itemEntity.item
    }

    /** Distinguishes a full inventory from a pickup that another mod refused, such as a pickup filter. */
    private fun isInventoryFull(stack: ItemStack, player: Player): Boolean = try {
        !canAddItem(stack, player)
    } catch (_: RuntimeException) {
        true
    }

    private fun canAddItem(stack: ItemStack, player: Player): Boolean {
        if (player.inventory.freeSlot >= 0) return true
        if (player.hasInfiniteMaterials()) return true
        if (stack.isDamaged) return false
        return player.inventory.getSlotWithRemainingSpace(stack) > -1
    }

    private enum class AlertType {
        SOUND,
        TEXT,
        TITLE
    }
}
