package dev.nyon.magnetic.compat

import com.zetaplugins.timberz.TimberZ
import com.zetaplugins.timberz.service.MaterialTypeChecks
import dev.nyon.magnetic.Animation
import dev.nyon.magnetic.DropAuthorization
import dev.nyon.magnetic.DropEntityDispatcher
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Integration for ZetaPlugins' TimberZ.
 *
 * TimberZ cancels the player's [BlockBreakEvent] on a log and breaks the whole tree over the following ticks with
 * `Block#breakNaturally`, which fires no block drop event. Its instant leaf decay and leaf insta-break work the same
 * way. Magnetic records the blocks TimberZ is about to break and collects the items that spawn there.
 *
 * On Folia, every event here fires on the thread of the region it happens in. Only blocks owned by that region are
 * read, and claims expire by wall-clock time because region ticks advance independently.
 */
object TimberZCompat {
    /**
     * TimberZ breaks one log per tick. Each log is allowed two ticks so lagging servers still finish within the
     * window, and logs stay claimable for a grace period after the last one is due.
     */
    private const val MILLIS_PER_LOG = 100L
    private const val LOG_GRACE_MILLIS = 2_000L

    /** TimberZ decays leaves 5 to 45 ticks (2.25 seconds) after the tree was felled; the window tolerates lag. */
    private const val LEAF_DECAY_MILLIS = 5_000L
    private const val DEFAULT_LEAF_SEARCH_RADIUS = 4

    private val handledDropKey = NamespacedKey("magnetic", "timberz_handled")

    private data class BlockKey(val worldId: UUID, val x: Int, val y: Int, val z: Int)

    private class Claim(
        val authorization: DropAuthorization,
        val deferToDatapack: Boolean,
        val expiresAt: Long
    )

    private class LeafBreak(val event: BlockDamageEvent, val type: Material) {
        // Resolved once the first item spawns, as most damaged blocks are not broken.
        val authorization: DropAuthorization? by lazy { DropEventDispatcher.authorize(event.player) }
        val deferToDatapack: Boolean by lazy { SmeltingDatapackCompat.defersDrops(event.player) }
    }

    private val claims = ConcurrentHashMap<BlockKey, Claim>()

    // TimberZ breaks leaves synchronously from its BlockDamageEvent listener, so the item spawns on the same thread.
    // That holds on Folia as well, where the event, the break and the spawn all run on the block's region thread.
    private val leafBreaks = ThreadLocal.withInitial { ArrayDeque<LeafBreak>() }

    fun register() {
        listenForEvents(Bukkit.getPluginManager().getPlugin("TimberZ") as? TimberZ ?: return)
    }

    private fun listenForEvents(timberZ: TimberZ) {
        // TimberZ cancels the break when it fells the tree, while the logs are still in place.
        listen<BlockBreakEvent>(EventPriority.MONITOR) {
            if (!isCancelled) return@listen
            try {
                registerFelledTree(timberZ, player, block)
            } catch (exception: RuntimeException) {
                timberZ.logger.fine("Magnetic could not inspect a TimberZ tree: $exception")
            } catch (_: LinkageError) {
                // An incompatible TimberZ version; its drops are left alone.
            }
        }

        listen<BlockDamageEvent>(EventPriority.LOWEST) {
            if (!timberZ.config.getBoolean("instaBreakLeavesWithTimber")) return@listen
            leafBreaks.get().addLast(LeafBreak(this, block.type))
        }

        // MONITOR handlers run even when the event was cancelled, so the capture is always closed here.
        listen<BlockDamageEvent>(EventPriority.MONITOR) {
            val stack = leafBreaks.get()
            val index = stack.indexOfLast { it.event === this }
            if (index >= 0) stack.removeAt(index)
            if (stack.isEmpty()) leafBreaks.remove()
        }

        listen<ItemSpawnEvent>(EventPriority.MONITOR) {
            if (isCancelled) return@listen
            val item = entity
            if (
                item.persistentDataContainer.has(handledDropKey) ||
                Animation.tracksItem(item) ||
                SmeltingDatapackCompat.isDeferred(item)
            ) {
                return@listen
            }

            val leafBreak = leafBreaks.get().lastOrNull()
            if (leafBreak != null) {
                collectLeafBreak(item, leafBreak)
                return@listen
            }

            if (claims.isEmpty()) return@listen
            val key = item.location.positionKey()
            val claim = claims[key] ?: return@listen
            if (claim.expiresAt < System.currentTimeMillis()) {
                claims.remove(key, claim)
                return@listen
            }

            collect(item, claim.authorization, claim.deferToDatapack)
        }
    }

    private fun registerFelledTree(timberZ: TimberZ, player: Player, block: Block) {
        if (player.isSneaking) return
        val detection = timberZ.treeDetectionService
        val logType = block.type
        if (!detection.containsLog(logType)) return
        if (!MaterialTypeChecks.isValidAxe(player.inventory.itemInMainHand, timberZ)) return

        val ownership = RegionOwnership()
        val logs = detection.identifyTreeStructure(block).filter(ownership::owns)
        if (logs.isEmpty()) return
        val authorization = DropEventDispatcher.authorize(player) ?: return

        val now = System.currentTimeMillis()
        claims.entries.removeIf { it.value.expiresAt < now }

        val deferToDatapack = SmeltingDatapackCompat.defersDrops(player)
        val logClaim = Claim(authorization, deferToDatapack, now + logs.size * MILLIS_PER_LOG + LOG_GRACE_MILLIS)
        logs.forEach { log ->
            if (!log.type.isIgnored) claims[log.positionKey()] = logClaim
        }

        val leafMillis = leafDecayMillis(timberZ)
        if (leafMillis <= 0) return
        val leafTypes = detection.getLeafTypes(logType)?.toSet().orEmpty()
        if (leafTypes.isEmpty()) return

        val leafClaim = Claim(authorization, deferToDatapack, now + leafMillis)
        val radius = timberZ.config.getInt("leavesSearchRadius", DEFAULT_LEAF_SEARCH_RADIUS).coerceAtLeast(0)
        val visited = hashSetOf<BlockKey>()
        logs.forEach { log ->
            for (x in -radius..radius) for (y in -radius..radius) for (z in -radius..radius) {
                val leaf = log.getRelative(x, y, z)
                val key = leaf.positionKey()
                if (!visited.add(key) || !ownership.owns(leaf)) continue
                if (leaf.type in leafTypes && !leaf.type.isIgnored) claims.putIfAbsent(key, leafClaim)
            }
        }
    }

    /** How long TimberZ's instant leaf decay is collected, following Magnetic's own leaf decay setting. */
    private fun leafDecayMillis(timberZ: TimberZ): Long {
        if (!config.leafDecay.enabled || !timberZ.config.getBoolean("instantLeafDecay")) return 0
        return config.leafDecay.abilityTimeout.coerceIn(0L, LEAF_DECAY_MILLIS)
    }

    /**
     * Caches whether chunks belong to the current thread's region. Folia forbids reading blocks of other regions, so
     * those blocks are not claimed and their drops stay on the ground. Always true on Paper.
     */
    private class RegionOwnership {
        private val chunks = hashMapOf<Long, Boolean>()

        fun owns(block: Block): Boolean {
            val chunkX = block.x shr 4
            val chunkZ = block.z shr 4
            val chunkKey = (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)
            return chunks.getOrPut(chunkKey) { Bukkit.isOwnedByCurrentRegion(block.world, chunkX, chunkZ) }
        }
    }

    private fun collectLeafBreak(item: Item, leafBreak: LeafBreak) {
        if (leafBreak.type.isIgnored) return
        val authorization = leafBreak.authorization ?: return
        collect(item, authorization, leafBreak.deferToDatapack)
    }

    private fun collect(item: Item, authorization: DropAuthorization, deferToDatapack: Boolean) {
        // Auto-smelt datapacks still need to smelt the items.
        if (deferToDatapack) {
            SmeltingDatapackCompat.collectAfterDatapack(item, authorization)
            return
        }

        item.persistentDataContainer.set(handledDropKey, PersistentDataType.BYTE, 1.toByte())
        DropEntityDispatcher.claim(item) { claimedDrop ->
            DropEntityDispatcher.dispatch(claimedDrop, authorization, handledDropKey)
        }
    }

    private fun Block.positionKey() = BlockKey(world.uid, x, y, z)

    private fun Location.positionKey() = BlockKey(world.uid, blockX, blockY, blockZ)
}
