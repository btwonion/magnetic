package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.Animation
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.canAddItem
import dev.nyon.magnetic.extensions.listen
import org.bukkit.Statistic
import org.bukkit.entity.Player
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object DropEventListener {

    @Suppress("unused")
    private val magneticListener = listen<DropEvent> {
        if (!config.conditionStatement.checkAndReport(player)) return@listen

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (config.animation.enabled && player.canAddItem(item)) {
                    Animation.pullItemToPlayer(item, pos.toCenterLocation(), player)
                    return@removeIf true
                }

                val copiedStack = item.clone()
                if (player.inventory.addItem(item).isNotEmpty()) {
                    tickInventoryAlert(player)
                    return@removeIf false
                }
                if (copiedStack.amount > 0) player.incrementStatistic(
                    Statistic.PICKUP, copiedStack.type, copiedStack.amount
                )
                true
            }
        }
        if (config.expAllowed) {
            player.giveExp(exp.value, true)
            exp.value = 0
        }
    }

    private val cooldowns: Map<Config.FullInventoryAlert.Alert, MutableMap<UUID, Instant>> = mapOf(
        config.fullInventoryAlert.soundAlert to mutableMapOf(),
        config.fullInventoryAlert.textAlert to mutableMapOf(),
        config.fullInventoryAlert.titleAlert to mutableMapOf()
    )

    private fun tickInventoryAlert(player: Player) {
        val currentTime = Clock.System.now()
        val uuid = player.playerProfile.id ?: return
        cooldowns.forEach { (alert, playerCooldowns) ->
            if (!alert.enabled) return@forEach
            val lastAlert = playerCooldowns[uuid]
            if (lastAlert == null || currentTime > lastAlert + alert.cooldownInSeconds.seconds) {
                playerCooldowns[uuid] = currentTime
                alert.invoke(player)
            }
        }
    }
}