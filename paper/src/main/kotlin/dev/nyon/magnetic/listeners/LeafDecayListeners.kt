package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropAuthorization
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Leaves
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.EntitySnapshot
import org.bukkit.entity.Item
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

internal val leafDecayHandledDropKey = NamespacedKey("magnetic", "leaf_decay_handled")

@Suppress("unused")
object LeafDecayListeners {

    private data class BlockKey(
        val worldId: UUID,
        val x: Int,
        val y: Int,
        val z: Int
    )

    private data class Entry(
        val authorization: DropAuthorization,
        val expiresAt: Long,
        val generation: Long
    )

    private data class LeafState(
        val maximumDistance: Int,
        val persistent: Boolean
    )

    private data class ClaimedDrop(
        val location: Location,
        val itemStack: ItemStack,
        val entitySnapshot: EntitySnapshot
    )

    private val directions = listOf(
        BlockFace.DOWN,
        BlockFace.UP,
        BlockFace.NORTH,
        BlockFace.SOUTH,
        BlockFace.WEST,
        BlockFace.EAST
    )
    private val trackedLeaves = ConcurrentHashMap<BlockKey, Entry>()
    private val pendingDecay = ConcurrentHashMap<BlockKey, Entry>()
    private val activeScans = ConcurrentHashMap<Long, CanopyScan>()
    private val earlyDecays = ConcurrentHashMap<BlockKey, EarlyDecay>()
    private val generation = AtomicLong()

    private val blockBreakEvent = listen<BlockBreakEvent>(EventPriority.MONITOR) {
        if (isCancelled || !config.leafDecay.enabled) return@listen
        if (!Tag.LOGS.isTagged(block.type) || block.type.isIgnored) return@listen
        val authorization = DropEventDispatcher.authorize(player) ?: return@listen

        val now = System.currentTimeMillis()
        val timeout = config.leafDecay.abilityTimeout.coerceAtLeast(0)
        val expiresAt = if (timeout > Long.MAX_VALUE - now) Long.MAX_VALUE else now + timeout
        if (expiresAt <= now) return@listen

        CanopyScan(
            block.world,
            block.key(),
            Entry(authorization, expiresAt, generation.incrementAndGet())
        ).start()
    }

    private val leavesDecayEvent = listen<LeavesDecayEvent>(EventPriority.MONITOR) {
        if (isCancelled || !config.leafDecay.enabled) return@listen
        if (block.type.isIgnored) return@listen

        val key = block.key()
        val decayedAt = System.currentTimeMillis()
        val entry = activeEntry(key, decayedAt)
        val leafData = block.blockData as? Leaves ?: return@listen
        val buffered = bufferEarlyDecay(
            key,
            LeafState(leafData.maximumDistance, leafData.isPersistent),
            block.location,
            entry,
            decayedAt
        )
        if (buffered) {
            if (entry != null) trackedLeaves.remove(key, entry)
            return@listen
        }

        if (entry != null) {
            promotePendingDecay(key, entry, block.location)
            return@listen
        }

        // A scan may have completed between the first lookup and registration.
        activeEntry(key)?.let { promotePendingDecay(key, it, block.location) }
    }

    private val itemSpawnEvent = listen<ItemSpawnEvent>(EventPriority.MONITOR) {
        if (isCancelled) return@listen
        if (entity.persistentDataContainer.has(leafDecayHandledDropKey)) return@listen

        val dropLocation = entity.location.clone()
        val key = dropLocation.key()
        val entry = pendingDecay[key]
        val earlyDecay = earlyDecays[key]
        if (entry == null && earlyDecay == null) return@listen

        if (entry != null && Bukkit.getPlayer(entry.authorization.playerId) == null) return@listen

        entity.persistentDataContainer.set(
            leafDecayHandledDropKey,
            PersistentDataType.BYTE,
            1.toByte()
        )
        claimDrop(entity) { claimedDrop ->
            if (entry != null) {
                dispatchClaimedDrop(claimedDrop, entry)
            } else {
                earlyDecay!!.accept(claimedDrop)
            }
        }
    }

    private val cleanupTask = Main.INSTANCE.server.asyncScheduler.runAtFixedRate(
        Main.INSTANCE,
        {
            val now = System.currentTimeMillis()
            trackedLeaves.entries.removeIf { it.value.expiresAt <= now }
        },
        1L,
        1L,
        TimeUnit.SECONDS
    )

    private fun activeEntry(key: BlockKey, at: Long = System.currentTimeMillis()): Entry? {
        val entry = trackedLeaves[key] ?: return null
        if (entry.expiresAt > at) return entry
        trackedLeaves.remove(key, entry)
        return null
    }

    private fun promotePendingDecay(key: BlockKey, entry: Entry, location: Location) {
        trackedLeaves.remove(key, entry)
        pendingDecay[key] = entry

        Main.INSTANCE.server.regionScheduler.runDelayed(Main.INSTANCE, location, {
            pendingDecay.remove(key, entry)
        }, 1L)
    }

    private fun bufferEarlyDecay(
        key: BlockKey,
        leafState: LeafState,
        location: Location,
        existingEntry: Entry?,
        decayedAt: Long
    ): Boolean {
        val earlyDecay = EarlyDecay(existingEntry, decayedAt)
        activeScans.values
            .filter { it.couldContain(key) }
            .forEach { it.registerEarlyDecay(key, leafState, earlyDecay) }
        if (!earlyDecay.hasRegistrations()) return false

        earlyDecays[key] = earlyDecay
        Main.INSTANCE.server.regionScheduler.runDelayed(Main.INSTANCE, location, {
            earlyDecays.remove(key, earlyDecay)
        }, 1L)
        earlyDecay.closeRegistrations()
        return true
    }

    private fun claimDrop(entity: Item, onClaimed: (ClaimedDrop) -> Unit) {
        entity.scheduler.execute(Main.INSTANCE, {
            if (!entity.isValid) return@execute

            val entitySnapshot = entity.createSnapshot() ?: return@execute
            val claimedDrop = ClaimedDrop(
                entity.location.clone(),
                entity.itemStack.clone(),
                entitySnapshot
            )
            entity.remove()
            onClaimed(claimedDrop)
        }, null, 0L)
    }

    private fun dispatchClaimedDrop(claimedDrop: ClaimedDrop, entry: Entry?) {
        if (entry == null) {
            restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack))
            return
        }

        val player = Bukkit.getPlayer(entry.authorization.playerId)
        if (player == null) {
            restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack))
            return
        }

        val restored = AtomicBoolean()
        val restoreOnFailure = {
            if (restored.compareAndSet(false, true)) {
                restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack))
            }
        }
        val scheduled = player.scheduler.execute(Main.INSTANCE, {
            if (player.world.uid != claimedDrop.location.world.uid) {
                restoreOnFailure()
                return@execute
            }

            val itemStacks = mutableListOf(claimedDrop.itemStack.clone())
            DropEventDispatcher.callAuthorized(
                DropEvent(
                    itemStacks,
                    MutableInt(),
                    player,
                    claimedDrop.location,
                    leafDecayHandledDropKey
                ),
                entry.authorization
            )
            restoreResidualDrops(claimedDrop, itemStacks)
        }, restoreOnFailure, 0L)
        if (!scheduled) restoreOnFailure()
    }

    private fun restoreResidualDrops(claimedDrop: ClaimedDrop, items: List<ItemStack>) {
        val residual = items
            .filter { !it.type.isAir && it.amount > 0 }
            .map(ItemStack::clone)
        if (residual.isEmpty()) return

        Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, claimedDrop.location) {
            residual.forEach { restoreDropWithoutEvent(claimedDrop, it) }
        }
    }

    private fun restoreDropWithoutEvent(claimedDrop: ClaimedDrop, itemStack: ItemStack) {
        val world = claimedDrop.location.world as CraftWorld
        val item = claimedDrop.entitySnapshot.createEntity(world) as Item
        val handle = (item as CraftEntity).handle
        handle.setPos(claimedDrop.location.x, claimedDrop.location.y, claimedDrop.location.z)
        item.itemStack = itemStack
        item.persistentDataContainer.set(
            leafDecayHandledDropKey,
            PersistentDataType.BYTE,
            1.toByte()
        )

        // A null spawn reason is Paper's event-free re-add path. Other plugins already
        // observed and modified the original ItemSpawnEvent, so restoring a residual
        // through that public event pipeline would apply their transformations twice.
        world.addEntityToWorld(handle, null)
    }

    private class EarlyDecay(existingEntry: Entry?, val decayedAt: Long) {
        private var registrationsOpen = true
        private var registeredScans = 0
        private var resolvedScans = 0
        private var bestEntry: Entry? = existingEntry
        private var completed = false
        private val waitingDrops = mutableListOf<ClaimedDrop>()

        fun registerScan() = synchronized(this) {
            check(registrationsOpen) { "Cannot register a completed canopy scan." }
            registeredScans++
        }

        fun hasRegistrations() = synchronized(this) { registeredScans > 0 }

        fun closeRegistrations() {
            val completion = synchronized(this) {
                registrationsOpen = false
                completeIfReady()
            }
            completion?.dispatch()
        }

        fun resolve(entry: Entry?) {
            val completion = synchronized(this) {
                if (entry != null && (bestEntry == null || entry.generation > bestEntry!!.generation)) {
                    bestEntry = entry
                }
                resolvedScans++
                completeIfReady()
            }
            completion?.dispatch()
        }

        fun accept(claimedDrop: ClaimedDrop) {
            val entry = synchronized(this) {
                if (!completed) {
                    waitingDrops.add(claimedDrop)
                    return
                }
                bestEntry
            }
            dispatchClaimedDrop(claimedDrop, entry)
        }

        private fun completeIfReady(): Completion? {
            if (registrationsOpen || completed || resolvedScans < registeredScans) return null
            completed = true
            return Completion(waitingDrops.toList(), bestEntry).also { waitingDrops.clear() }
        }

        private data class Completion(
            val drops: List<ClaimedDrop>,
            val entry: Entry?
        ) {
            fun dispatch() {
                drops.forEach { dispatchClaimedDrop(it, entry) }
            }
        }
    }

    private class CanopyScan(
        private val world: World,
        private val removedLog: BlockKey,
        private val entry: Entry
    ) {
        private val depths = ConcurrentHashMap<BlockKey, Int>()
        private val leaves = ConcurrentHashMap<BlockKey, LeafState>()
        private val logs = ConcurrentHashMap.newKeySet<BlockKey>()
        private val earlyLeaves = ConcurrentHashMap<BlockKey, LeafState>()
        private val registeredEarlyDecays = ConcurrentHashMap<BlockKey, EarlyDecay>()
        private val pendingReads = AtomicInteger()
        private var replacementLogChecked = false
        private var finished = false

        fun start() {
            activeScans[entry.generation] = this
            pendingReads.incrementAndGet()
            directions.forEach { scheduleRead(removedLog.relative(it), 1) }
            if (pendingReads.decrementAndGet() == 0) finishIfReady()
        }

        fun couldContain(key: BlockKey): Boolean {
            if (key.worldId != removedLog.worldId) return false
            val distance = abs(key.x - removedLog.x) + abs(key.y - removedLog.y) + abs(key.z - removedLog.z)
            return distance < MAXIMUM_LEAF_DISTANCE
        }

        fun registerEarlyDecay(key: BlockKey, leafState: LeafState, earlyDecay: EarlyDecay): Boolean =
            synchronized(this) {
                if (finished || !couldContain(key)) return@synchronized false

                earlyDecay.registerScan()
                earlyLeaves[key] = leafState
                registeredEarlyDecays[key] = earlyDecay
                depths[key]?.let { recordLeaf(key, leafState, it) }
                true
            }

        private fun finishIfReady() = synchronized(this) {
            if (finished || pendingReads.get() != 0) return@synchronized
            if (!replacementLogChecked) {
                replacementLogChecked = true
                scheduleReplacementLogCheck()
                return@synchronized
            }
            finish()
            finished = true
            activeScans.remove(entry.generation, this)
        }

        private fun scheduleReplacementLogCheck() {
            pendingReads.incrementAndGet()
            Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, removedLog.location(world)) {
                try {
                    val block = world.getBlockAt(removedLog.x, removedLog.y, removedLog.z)
                    if (Tag.LOGS.isTagged(block.type)) logs.add(removedLog)
                } finally {
                    if (pendingReads.decrementAndGet() == 0) finishIfReady()
                }
            }
        }

        private fun scheduleRead(key: BlockKey, depth: Int) {
            if (key == removedLog || depth > CANOPY_SCAN_RADIUS) return
            if (key.y < world.minHeight || key.y >= world.maxHeight) return

            var shouldRead = false
            depths.compute(key) { _, currentDepth ->
                if (currentDepth == null || depth < currentDepth) {
                    shouldRead = true
                    depth
                } else {
                    currentDepth
                }
            }
            if (!shouldRead) return

            pendingReads.incrementAndGet()
            Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, key.location(world)) {
                try {
                    scanBlock(key, depth)
                } finally {
                    if (pendingReads.decrementAndGet() == 0) finishIfReady()
                }
            }
        }

        private fun scanBlock(key: BlockKey, scheduledDepth: Int) {
            val depth = depths[key] ?: return
            if (scheduledDepth > depth) return

            val earlyLeaf = earlyLeaves[key]
            if (earlyLeaf != null) {
                recordLeaf(key, earlyLeaf, depth)
                return
            }

            val block = world.getBlockAt(key.x, key.y, key.z)
            if (Tag.LOGS.isTagged(block.type)) {
                logs.add(key)
                return
            }
            if (!Tag.LEAVES.isTagged(block.type)) return

            val leafData = block.blockData as? Leaves ?: return
            recordLeaf(key, LeafState(leafData.maximumDistance, leafData.isPersistent), depth)
        }

        private fun recordLeaf(key: BlockKey, leafState: LeafState, depth: Int) {
            leaves[key] = leafState
            if (depth >= CANOPY_SCAN_RADIUS) return

            directions.forEach { scheduleRead(key.relative(it), depth + 1) }
        }

        private fun finish() {
            val candidates = hashSetOf<BlockKey>()
            if (config.leafDecay.enabled) {
                leaves.forEach { (key, leaf) ->
                    if (!isDecayCandidate(key, leaf)) return@forEach
                    if (!hasLogSupport(key, leaf.maximumDistance)) candidates.add(key)
                }
            }

            if (entry.expiresAt > System.currentTimeMillis()) {
                candidates
                    .filterNot(registeredEarlyDecays::containsKey)
                    .forEach { key ->
                        trackedLeaves.compute(key) { _, current ->
                            if (current == null || current.generation < entry.generation) entry else current
                        }
                    }
            }

            registeredEarlyDecays.forEach { (key, earlyDecay) ->
                // The decay event already proves that this leaf was unsupported. A log
                // observed later by the asynchronous scan must not invalidate that event.
                earlyDecay.resolve(entry.takeIf {
                    val leaf = leaves[key]
                    config.leafDecay.enabled && leaf != null && isDecayCandidate(key, leaf) &&
                        entry.expiresAt > earlyDecay.decayedAt
                })
            }
        }

        private fun isDecayCandidate(key: BlockKey, leaf: LeafState): Boolean {
            val depthFromRemovedLog = depths[key] ?: return false
            return !leaf.persistent && depthFromRemovedLog < leaf.maximumDistance
        }

        private fun hasLogSupport(leaf: BlockKey, maximumDistance: Int): Boolean {
            val visited = hashSetOf(leaf)
            val queue = ArrayDeque<Pair<BlockKey, Int>>()
            queue.add(leaf to 0)

            while (queue.isNotEmpty()) {
                val (current, depth) = queue.removeFirst()
                if (depth >= maximumDistance - 1) continue

                for (direction in directions) {
                    val neighbor = current.relative(direction)
                    if (!visited.add(neighbor)) continue
                    if (logs.contains(neighbor)) return true
                    if (leaves.containsKey(neighbor)) {
                        queue.add(neighbor to depth + 1)
                    }
                }
            }
            return false
        }
    }

    private fun Block.key() = BlockKey(world.uid, x, y, z)

    private fun org.bukkit.Location.key() = BlockKey(world.uid, blockX, blockY, blockZ)

    private fun BlockKey.relative(direction: BlockFace) = BlockKey(
        worldId,
        x + direction.modX,
        y + direction.modY,
        z + direction.modZ
    )

    private fun BlockKey.location(world: World) = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

    private const val MAXIMUM_LEAF_DISTANCE = 7
    private const val CANOPY_SCAN_RADIUS = (MAXIMUM_LEAF_DISTANCE - 1) * 2
}
