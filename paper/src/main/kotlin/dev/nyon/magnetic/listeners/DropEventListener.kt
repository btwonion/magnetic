package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.Animation
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.compat.autosmelt.AutoSmeltSession
import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.canAddItem
import dev.nyon.magnetic.extensions.listen
import org.bukkit.Statistic
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

object DropEventListener {

    @Suppress("unused")
    private val magneticListener = listen<DropEvent> {
        if (!DropEventDispatcher.isAuthorized(this) && !config.conditionStatement.checkAndReport(player)) {
            return@listen
        }

        // Auto-smelt datapacks only act on spawned item entities, so smelt block drops that Magnetic collects.
        val smelting = if (config.itemsAllowed && blockDrop) AutoSmeltSession.start(items, player) else null

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (config.animation.enabled && player.canAddItem(item)) {
                    Animation.pullItemToPlayer(
                        item,
                        pos.toCenterLocation(),
                        player,
                        itemSpawnMarker,
                        smelting?.isSmelted(item) == true
                    )
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
        val smeltingExperience = smelting?.finish(items) ?: 0
        if (smeltingExperience > 0) {
            if (config.expAllowed) {
                exp.add(smeltingExperience)
            } else {
                pos.world.spawn(pos.toCenterLocation(), ExperienceOrb::class.java) { it.experience = smeltingExperience }
            }
        }

        if (config.expAllowed) {
            player.giveExp(exp.toInt(), true)
            exp.value = 0
        }
    }

    // Drop events run on each player's region thread on Folia, so the cooldowns must be thread-safe.
    private val cooldowns: Map<Config.FullInventoryAlert.Alert, MutableMap<UUID, Instant>> = mapOf(
        config.fullInventoryAlert.soundAlert to ConcurrentHashMap(),
        config.fullInventoryAlert.textAlert to ConcurrentHashMap(),
        config.fullInventoryAlert.titleAlert to ConcurrentHashMap()
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
