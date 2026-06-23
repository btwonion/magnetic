package dev.nyon.magnetic.compat

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.SingleListener
import dev.nyon.magnetic.extensions.listen
import dev.nyon.magnetic.extensions.unregister
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object TreeCapitatorCompat {
    private const val SESSION_RANGE = 24.0
    private const val SESSION_TTL_MS = 1500L
    private const val SKIP_TTL_MS = 250L

    private data class BlockKey(val worldId: UUID, val x: Int, val y: Int, val z: Int)
    private data class Session(
        val player: Player,
        val location: Location,
        val expiresAt: Instant,
        val markerPositions: MutableSet<BlockKey> = mutableSetOf(),
        var markersLoaded: Boolean = false
    )

    private data class SpawnSkip(val stack: ItemStack, val key: BlockKey, val expiresAt: Instant)

    private val sessionsByWorld = mutableMapOf<UUID, MutableList<Session>>()
    private val spawnSkipsByWorld = mutableMapOf<UUID, MutableList<SpawnSkip>>()
    private var blockBreakListener: SingleListener<BlockBreakEvent>? = null
    private var itemSpawnListener: SingleListener<ItemSpawnEvent>? = null
    private var treeCapitatorEnabled = false

    fun listenForEvents() {
        listen<ServerLoadEvent> {
            refreshDatapackState()
        }
    }

    private fun refreshDatapackState() {
        treeCapitatorEnabled = hasTreeCapitatorDatapack()
        if (!treeCapitatorEnabled) {
            sessionsByWorld.clear()
            spawnSkipsByWorld.clear()
            unregisterDropListeners()
            return
        }
        registerDropListeners()
    }

    private fun registerDropListeners() {
        if (blockBreakListener != null || itemSpawnListener != null) return

        blockBreakListener = listen<BlockBreakEvent>(EventPriority.MONITOR) {
            if (!treeCapitatorEnabled) return@listen
            if (isCancelled || !block.type.isTreeCapitatorRoot()) return@listen
            if (!config.conditionStatement.checkAndReport(player)) return@listen

            cleanup()
            val worldId = block.world.uid
            sessionsByWorld.getOrPut(worldId, ::mutableListOf)
                .add(Session(player, block.location.toCenterLocation(), Clock.System.now() + SESSION_TTL_MS.milliseconds))
        }

        itemSpawnListener = listen<ItemSpawnEvent>(EventPriority.HIGHEST) {
            if (!treeCapitatorEnabled) return@listen
            cleanup()
            val worldId = location.world?.uid ?: return@listen

            if (sessionsByWorld[worldId].isNullOrEmpty() && spawnSkipsByWorld[worldId].isNullOrEmpty()) return@listen
            if (consumeSpawnSkip(entity)) return@listen

            val itemKey = location.toMagneticBlockKey()
            val session = (sessionsByWorld[worldId] ?: return@listen)
                .asSequence()
                .filter { it.location.distanceSquared(location) <= SESSION_RANGE * SESSION_RANGE }
                .onEach { it.loadMarkerPositions() }
                .filter { itemKey in it.markerPositions }
                .minByOrNull { it.location.distanceSquared(location) }
                ?: return@listen

            val items = mutableListOf(entity.itemStack)
            if (config.animation.enabled) {
                spawnSkipsByWorld.getOrPut(worldId, ::mutableListOf)
                    .add(SpawnSkip(entity.itemStack.clone(), location.toMagneticBlockKey(), Clock.System.now() + SKIP_TTL_MS.milliseconds))
            }

            DropEvent(items, MutableInt(), session.player, location).also(Event::callEvent)

            if (items.isEmpty()) {
                isCancelled = true
                entity.remove()
            } else {
                entity.itemStack = items.first()
            }
        }
    }

    private fun unregisterDropListeners() {
        blockBreakListener?.unregister()
        itemSpawnListener?.unregister()
        blockBreakListener = null
        itemSpawnListener = null
    }

    private fun Session.loadMarkerPositions() {
        if (markersLoaded) return

        location.world?.getNearbyEntities(location, SESSION_RANGE, SESSION_RANGE, SESSION_RANGE)?.forEach { entity ->
            if (entity.type == EntityType.MARKER && entity.scoreboardTags.any { it == "TC_Log" || it == "TC_Leaf" }) {
                markerPositions.add(entity.location.toMagneticBlockKey())
            }
        }
        markersLoaded = markerPositions.isNotEmpty()
    }

    private fun Material.isTreeCapitatorRoot(): Boolean {
        return name.endsWith("_LOG") || name.endsWith("_STEM")
    }

    private fun consumeSpawnSkip(item: Item): Boolean {
        val skips = spawnSkipsByWorld[item.world.uid] ?: return false
        val itemKey = item.location.toMagneticBlockKey()
        val match = skips.firstOrNull { skip ->
            skip.key == itemKey && skip.stack.isSimilar(item.itemStack)
        } ?: return false

        skips.remove(match)
        return true
    }

    private fun cleanup() {
        val now = Clock.System.now()
        sessionsByWorld.entries.removeIf { (_, sessions) ->
            sessions.removeIf { it.expiresAt < now || !it.player.isOnline }
            sessions.isEmpty()
        }
        spawnSkipsByWorld.entries.removeIf { (_, skips) ->
            skips.removeIf { it.expiresAt < now }
            skips.isEmpty()
        }
    }

    private fun hasTreeCapitatorDatapack(): Boolean {
        return Bukkit.getDatapackManager().enabledPacks.any { pack ->
            val name = pack.name.lowercase()
            val title = PlainTextComponentSerializer.plainText().serialize(pack.title).lowercase()
            val description = PlainTextComponentSerializer.plainText().serialize(pack.description).lowercase()
            name.contains("treecapitator") ||
                title.contains("treecapitator") ||
                name.contains("tree_capitator") ||
                title.contains("tree capitator") ||
                description.contains("cut trees in one go")
        }
    }

    private fun Location.toMagneticBlockKey(): BlockKey {
        return BlockKey(world.uid, blockX, blockY, blockZ)
    }
}
