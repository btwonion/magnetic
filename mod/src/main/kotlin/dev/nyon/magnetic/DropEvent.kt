package dev.nyon.magnetic

import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.centerVec
import dev.nyon.magnetic.mixins.ExperienceOrbInvoker
import dev.nyon.magnetic.utils.CooldownTracker
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.ExperienceOrb
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
        if (!config.conditionStatement.checkAndReport(player)) return

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (config.animation.enabled && canAddItem(item, player)) {
                    Animation.pullItemToPlayer(item, pos.centerVec(), player)
                    return@removeIf true
                }

                if (item.isEmpty) return@removeIf true
                val copiedStack = item.copy()
                if (!player.addItem(item)) {
                    tickInventoryAlert(player)
                    return@removeIf false
                }
                player.awardStat(Stats.ITEM_PICKED_UP.get(copiedStack.item), copiedStack.count)
                true
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
