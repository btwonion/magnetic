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
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import net.minecraft.world.entity.item.ItemEntity
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

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
        val velocity: Vector,
        val pickupDelay: Int,
        val owner: UUID?,
        val thrower: UUID?,
        val canMobPickup: Boolean,
        val unlimitedLifetime: Boolean,
        val willAge: Boolean,
        val health: Int
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
        val entry = activeEntry(key) ?: return@listen
        trackedLeaves.remove(key, entry)
        pendingDecay[key] = entry

        Main.INSTANCE.server.regionScheduler.runDelayed(Main.INSTANCE, block.location, {
            pendingDecay.remove(key, entry)
        }, 1L)
    }

    private val itemSpawnEvent = listen<ItemSpawnEvent>(EventPriority.MONITOR) {
        if (isCancelled) return@listen
        if (entity.persistentDataContainer.has(leafDecayHandledDropKey)) return@listen

        val dropLocation = entity.location.clone()
        val entry = pendingDecay[dropLocation.key()] ?: return@listen
        val player = Bukkit.getPlayer(entry.authorization.playerId) ?: return@listen
        if (player.world.uid != dropLocation.world.uid) return@listen

        entity.persistentDataContainer.set(
            leafDecayHandledDropKey,
            PersistentDataType.BYTE,
            1.toByte()
        )
        claimDrop(entity, player, entry.authorization)
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

    private fun activeEntry(key: BlockKey): Entry? {
        val entry = trackedLeaves[key] ?: return null
        if (entry.expiresAt > System.currentTimeMillis()) return entry
        trackedLeaves.remove(key, entry)
        return null
    }

    private fun claimDrop(entity: Item, player: Player, authorization: DropAuthorization) {
        entity.scheduler.execute(Main.INSTANCE, {
            if (!entity.isValid) return@execute

            val claimedDrop = ClaimedDrop(
                entity.location.clone(),
                entity.itemStack.clone(),
                entity.velocity.clone(),
                entity.pickupDelay,
                entity.owner,
                entity.thrower,
                entity.canMobPickup(),
                entity.isUnlimitedLifetime,
                entity.willAge(),
                entity.health
            )
            entity.remove()

            player.scheduler.execute(Main.INSTANCE, {
                val itemStacks = mutableListOf(claimedDrop.itemStack.clone())
                DropEventDispatcher.callAuthorized(
                    DropEvent(
                        itemStacks,
                        MutableInt(),
                        player,
                        claimedDrop.location,
                        leafDecayHandledDropKey
                    ),
                    authorization
                )
                restoreResidualDrops(claimedDrop, itemStacks)
            }, {
                restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack))
            }, 0L)
        }, null, 0L)
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
        val handle = ItemEntity(
            world.handle,
            claimedDrop.location.x,
            claimedDrop.location.y,
            claimedDrop.location.z,
            CraftItemStack.asNMSCopy(itemStack),
            claimedDrop.velocity.x,
            claimedDrop.velocity.y,
            claimedDrop.velocity.z
        )
        val item = handle.bukkitEntity as Item
        item.pickupDelay = claimedDrop.pickupDelay
        item.owner = claimedDrop.owner
        item.thrower = claimedDrop.thrower
        item.setCanMobPickup(claimedDrop.canMobPickup)
        item.isUnlimitedLifetime = claimedDrop.unlimitedLifetime
        item.setWillAge(claimedDrop.willAge)
        item.health = claimedDrop.health
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

    private class CanopyScan(
        private val world: World,
        private val removedLog: BlockKey,
        private val entry: Entry
    ) {
        private val depths = ConcurrentHashMap<BlockKey, Int>()
        private val leaves = ConcurrentHashMap<BlockKey, LeafState>()
        private val logs = ConcurrentHashMap.newKeySet<BlockKey>()
        private val pendingReads = AtomicInteger()

        fun start() {
            pendingReads.incrementAndGet()
            directions.forEach { scheduleRead(removedLog.relative(it), 1) }
            if (pendingReads.decrementAndGet() == 0) finish()
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
                    if (pendingReads.decrementAndGet() == 0) finish()
                }
            }
        }

        private fun scanBlock(key: BlockKey, scheduledDepth: Int) {
            val depth = depths[key] ?: return
            if (scheduledDepth > depth) return

            val block = world.getBlockAt(key.x, key.y, key.z)
            if (Tag.LOGS.isTagged(block.type)) {
                logs.add(key)
                return
            }
            if (!Tag.LEAVES.isTagged(block.type)) return

            val leafData = block.blockData as? Leaves ?: return
            leaves[key] = LeafState(leafData.maximumDistance, leafData.isPersistent)
            if (depth >= CANOPY_SCAN_RADIUS) return

            directions.forEach { scheduleRead(key.relative(it), depth + 1) }
        }

        private fun finish() {
            if (!config.leafDecay.enabled || entry.expiresAt <= System.currentTimeMillis()) return

            leaves.forEach { (key, leaf) ->
                val depthFromRemovedLog = depths[key] ?: return@forEach
                if (leaf.persistent || depthFromRemovedLog >= leaf.maximumDistance) return@forEach
                if (hasLogSupport(key, leaf.maximumDistance)) return@forEach

                trackedLeaves.compute(key) { _, current ->
                    if (current == null || current.generation < entry.generation) entry else current
                }
            }
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
