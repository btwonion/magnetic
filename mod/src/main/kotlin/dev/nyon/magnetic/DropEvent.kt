package dev.nyon.magnetic

import dev.nyon.magnetic.common.AlertType
import dev.nyon.magnetic.common.FullInventoryAlerts
import dev.nyon.magnetic.config.conditions.checkAndReport
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.mixins.ExperienceOrbInvoker
import dev.nyon.magnetic.utils.MixinHelper
import dev.nyon.magnetic.utils.MixinHelper.conditionAlreadyChecked
import dev.nyon.magnetic.utils.ThreadLocalScope
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.stats.Stats
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt

object DropEvent {
    private val inventoryAlerts = FullInventoryAlerts()

    operator fun invoke(
        items: MutableList<ItemStack>,
        exp: MutableInt,
        player: ServerPlayer,
        pos: BlockPos
    ) {
        if (conditionAlreadyChecked.get() != true && !config.conditionStatement.checkAndReport(player)) return

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (item.isEmpty) return@removeIf true
                if (config.animation.enabled && canAddItemOrFalse(item, player)) {
                    Animation.pullItemToPlayer(item, Vec3.atCenterOf(pos), player)
                    return@removeIf true
                }

                val remaining = pickUp(item, player)
                if (remaining.isEmpty) return@removeIf true
                item.count = remaining.count
                if (!canAddItemOrFalse(remaining, player)) tickInventoryAlert(player)
                false
            }
        }

        if (config.expAllowed && exp.toInt() > 0) {
            val orb = ExperienceOrb(player.level(), 0.0, 0.0, 0.0, exp.toInt())
            val leftExp = (orb as ExperienceOrbInvoker).invokeRepairPlayerItems(player, exp.toInt())
            if (leftExp > 0) player.giveExperiencePoints(leftExp)
            exp.value = 0
        }
    }

    private fun tickInventoryAlert(player: ServerPlayer) {
        inventoryAlerts.due(config.fullInventoryAlert, player.uuid).forEach { type ->
            when (type) {
                AlertType.SOUND -> player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 1f)
                AlertType.TEXT -> player.sendSystemMessage(
                    Component.translatableWithFallback(
                        "chat.message.fullinventoryalert.text", "[Magnetic] Your inventory is full!"
                    ).withStyle(ChatFormatting.GOLD)
                )
                AlertType.TITLE -> {
                    player.connection.send(
                        ClientboundSetTitleTextPacket(
                            Component.translatableWithFallback("chat.message.fullinventoryalert.title.main", "!!!")
                                .withStyle(ChatFormatting.RED)
                        )
                    )
                    player.connection.send(
                        ClientboundSetSubtitleTextPacket(
                            Component.translatableWithFallback(
                                "chat.message.fullinventoryalert.title.subtitle", "Your inventory is full!"
                            ).withStyle(ChatFormatting.RED)
                        )
                    )
                }
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

    private fun canAddItemOrFalse(stack: ItemStack, player: Player): Boolean = try {
        player.inventory.freeSlot >= 0 ||
            player.hasInfiniteMaterials() ||
            !stack.isDamaged && player.inventory.getSlotWithRemainingSpace(stack) > -1
    } catch (_: RuntimeException) {
        false
    }
}
