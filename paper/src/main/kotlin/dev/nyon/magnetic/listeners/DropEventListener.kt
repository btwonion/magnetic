package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.Animation
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.common.AlertType
import dev.nyon.magnetic.common.FullInventoryAlerts
import dev.nyon.magnetic.config.conditions.checkAndReport
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.canAddItem
import dev.nyon.magnetic.extensions.listen
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Statistic
import org.bukkit.entity.Player

object DropEventListener {
    private val inventoryAlerts = FullInventoryAlerts()
    private val alertSound = Sound.sound(Key.key("block.note_block.pling"), Sound.Source.MASTER, 1f, 1f)

    @Suppress("unused")
    private val magneticListener = listen<DropEvent> {
        if (!DropEventDispatcher.isAuthorized(this) && !config.conditionStatement.checkAndReport(player)) {
            return@listen
        }

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (config.animation.enabled && player.canAddItem(item)) {
                    Animation.pullItemToPlayer(item, pos.toCenterLocation(), player, itemSpawnMarker)
                    return@removeIf true
                }

                val pickedUpType = item.type
                val originalAmount = item.amount
                val leftover = player.inventory.addItem(item)
                // Count whatever made it into the inventory, even when it filled up midway.
                val addedAmount = originalAmount - leftover.values.sumOf { it.amount }
                if (addedAmount > 0) player.incrementStatistic(Statistic.PICKUP, pickedUpType, addedAmount)
                if (leftover.isNotEmpty()) {
                    tickInventoryAlert(player)
                    return@removeIf false
                }
                true
            }
        }
        if (config.expAllowed) {
            player.giveExp(exp.toInt(), true)
            exp.value = 0
        }
    }

    private fun tickInventoryAlert(player: Player) {
        inventoryAlerts.due(config.fullInventoryAlert, player.uniqueId).forEach { type ->
            when (type) {
                AlertType.SOUND -> player.playSound(alertSound)
                AlertType.TEXT -> player.sendMessage(Component.translatable("chat.message.fullinventoryalert.text"))
                AlertType.TITLE -> player.showTitle(
                    Title.title(
                        Component.translatable("chat.message.fullinventoryalert.title.main"),
                        Component.translatable("chat.message.fullinventoryalert.title.subtitle")
                    )
                )
            }
        }
    }
}
