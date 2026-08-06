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
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Leaves
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
import java.util.concurrent.atomic.AtomicLong

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

    private data class LeafNode(val block: Block, val depth: Int)

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
    private val generatedDropKey = NamespacedKey("magnetic", "leaf_decay_generated")

    private val blockBreakEvent = listen<BlockBreakEvent>(EventPriority.MONITOR) {
        if (isCancelled || !config.leafDecay.enabled) return@listen
        if (!Tag.LOGS.isTagged(block.type) || block.type.isIgnored) return@listen
        val authorization = DropEventDispatcher.authorize(player) ?: return@listen

        val now = System.currentTimeMillis()
        val timeout = config.leafDecay.abilityTimeout.coerceAtLeast(0)
        val expiresAt = if (timeout > Long.MAX_VALUE - now) Long.MAX_VALUE else now + timeout
        if (expiresAt <= now) return@listen

        trackLeavesAround(
            block,
            Entry(authorization, expiresAt, generation.incrementAndGet())
        )
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
        if (entity.persistentDataContainer.has(generatedDropKey)) {
            entity.persistentDataContainer.remove(generatedDropKey)
            return@listen
        }

        val dropLocation = entity.location.clone()
        val entry = pendingDecay[dropLocation.key()] ?: return@listen
        val player = Bukkit.getPlayer(entry.authorization.playerId) ?: return@listen
        if (player.world.uid != dropLocation.world.uid) return@listen

        val originalItem = entity.itemStack.clone()
        isCancelled = true
        player.scheduler.execute(Main.INSTANCE, {
            val itemStacks = mutableListOf(originalItem)
            DropEventDispatcher.callAuthorized(
                DropEvent(itemStacks, MutableInt(), player, dropLocation, generatedDropKey),
                entry.authorization
            )
            itemStacks.forEach { spawnGeneratedDrop(dropLocation, it) }
        }, {
            spawnGeneratedDrop(dropLocation, originalItem)
        }, 0L)
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

    private fun trackLeavesAround(log: Block, entry: Entry) {
        val visited = hashSetOf(log.key())
        val queue = ArrayDeque<LeafNode>()
        directions.forEach { direction ->
            val leaf = log.getRelative(direction)
            if (visited.add(leaf.key())) queue.add(LeafNode(leaf, 1))
        }

        while (queue.isNotEmpty()) {
            val (leaf, depth) = queue.removeFirst()
            if (!Tag.LEAVES.isTagged(leaf.type)) continue

            val leaves = leaf.blockData as? Leaves ?: continue
            if (depth >= leaves.maximumDistance) continue

            trackedLeaves.compute(leaf.key()) { _, current ->
                if (current == null || current.generation < entry.generation) entry else current
            }

            directions.forEach { direction ->
                val neighbor = leaf.getRelative(direction)
                if (visited.add(neighbor.key())) queue.add(LeafNode(neighbor, depth + 1))
            }
        }
    }

    private fun activeEntry(key: BlockKey): Entry? {
        val entry = trackedLeaves[key] ?: return null
        if (entry.expiresAt > System.currentTimeMillis()) return entry
        trackedLeaves.remove(key, entry)
        return null
    }

    private fun spawnGeneratedDrop(location: Location, item: ItemStack) {
        Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, location) {
            location.world.dropItem(location, item) { itemEntity ->
                itemEntity.persistentDataContainer.set(
                    generatedDropKey,
                    PersistentDataType.BYTE,
                    1.toByte()
                )
            }
        }
    }

    private fun Block.key() = BlockKey(world.uid, x, y, z)

    private fun org.bukkit.Location.key() = BlockKey(world.uid, blockX, blockY, blockZ)
}
