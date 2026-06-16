package dev.nyon.magnetic.compat

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.inventory.ItemStack
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object TreeCapitatorCompat {
    private const val SESSION_RANGE = 24.0
    private const val MARKER_RANGE = 0.25
    private const val SESSION_TTL_MS = 1500L
    private const val SKIP_TTL_MS = 250L

    private data class Session(val player: Player, val location: Location, val expiresAt: Instant)
    private data class SpawnSkip(val stack: ItemStack, val location: Location, val expiresAt: Instant)

    private val sessions = mutableListOf<Session>()
    private val spawnSkips = mutableListOf<SpawnSkip>()

    fun listenForEvents() {
        listen<BlockBreakEvent>(EventPriority.MONITOR) {
            if (isCancelled || !block.type.isTreeCapitatorRoot()) return@listen
            if (!config.conditionStatement.checkAndReport(player)) return@listen

            cleanup()
            sessions.add(Session(player, block.location.toCenterLocation(), Clock.System.now() + SESSION_TTL_MS.milliseconds))
        }

        listen<ItemSpawnEvent>(EventPriority.HIGHEST) {
            cleanup()
            if (consumeSpawnSkip(entity)) return@listen
            if (!entity.isTreeCapitatorDrop()) return@listen

            val session = sessions
                .filter { it.location.world?.uid == location.world?.uid }
                .filter { it.location.distanceSquared(location) <= SESSION_RANGE * SESSION_RANGE }
                .minByOrNull { it.location.distanceSquared(location) }
                ?: return@listen

            val items = mutableListOf(entity.itemStack)
            if (config.animation.enabled) spawnSkips.add(SpawnSkip(entity.itemStack.clone(), location.toCenterLocation(), Clock.System.now() + SKIP_TTL_MS.milliseconds))

            DropEvent(items, MutableInt(), session.player, location).also(Event::callEvent)

            if (items.isEmpty()) {
                isCancelled = true
                entity.remove()
            } else {
                entity.itemStack = items.first()
            }
        }
    }

    private fun Item.isTreeCapitatorDrop(): Boolean {
        return getNearbyEntities(MARKER_RANGE, MARKER_RANGE, MARKER_RANGE).any { entity ->
            entity.type == EntityType.MARKER && (
                entity.scoreboardTags.contains("TC_Log") ||
                    entity.scoreboardTags.contains("TC_Leaf") ||
                    entity.customName() != null && entity.customName().toString().contains("TreeCapitator")
                )
        }
    }

    private fun Material.isTreeCapitatorRoot(): Boolean {
        return name.endsWith("_LOG") || name.endsWith("_STEM")
    }

    private fun consumeSpawnSkip(item: Item): Boolean {
        val match = spawnSkips.firstOrNull { skip ->
            skip.location.world?.uid == item.location.world?.uid &&
                skip.location.distanceSquared(item.location) <= MARKER_RANGE * MARKER_RANGE &&
                skip.stack.isSimilar(item.itemStack)
        } ?: return false

        spawnSkips.remove(match)
        return true
    }

    private fun cleanup() {
        val now = Clock.System.now()
        sessions.removeIf { it.expiresAt < now || !it.player.isOnline }
        spawnSkips.removeIf { it.expiresAt < now }
    }
}
