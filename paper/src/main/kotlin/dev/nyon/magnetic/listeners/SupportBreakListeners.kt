package dev.nyon.magnetic.listeners

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import dev.nyon.magnetic.DropAuthorization
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ExperienceOrb
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Collects blocks that break because a player broke what they stood on, hung from or were attached
 * to, such as torches, carpets, sugar cane columns and vines. Vanilla destroys those blocks itself,
 * in the same tick or a few ticks later, so their drops are not part of the player's
 * BlockDropItemEvent. Paper fires a BlockDestroyEvent before vanilla drops them.
 *
 * Breaking a block marks its neighbors for a short time. A marked block that is destroyed because it
 * can no longer stay where it is drops into the player's inventory and marks its own neighbors, so a
 * whole column or plant follows. Blocks that other plugins destroy while they could still stay, such
 * as the logs a tree feller breaks, are left to those plugins.
 */
@Suppress("unused")
object SupportBreakListeners {
    private data class BlockKey(val worldId: UUID, val x: Int, val y: Int, val z: Int)

    private class Mark(val authorization: DropAuthorization, val expiresAt: Long)

    // Vanilla breaks unsupported blocks within a few ticks of losing their support.
    private const val MARK_LIFETIME_MILLIS = 1000L
    private val directions = listOf(BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST)
    private val marks = ConcurrentHashMap<BlockKey, Mark>()

    private val blockBreakEvent = listen<BlockBreakEvent>(EventPriority.MONITOR) {
        if (isCancelled || block.type.isIgnored) return@listen
        val authorization = DropEventDispatcher.authorize(player) ?: return@listen
        markNeighbors(block, authorization)
    }

    private val blockDestroyEvent = listen<BlockDestroyEvent>(EventPriority.HIGHEST) {
        val mark = marks.remove(block.key()) ?: return@listen
        if (isCancelled || !willDrop() || mark.expiresAt < System.currentTimeMillis()) return@listen
        if (block.type.isIgnored || block.canPlace(block.blockData)) return@listen
        val player = Bukkit.getPlayer(mark.authorization.playerId) ?: return@listen

        val items = block.drops.toMutableList()
        val exp = MutableInt(expToDrop)
        setWillDrop(false)
        DropEventDispatcher.callAuthorized(DropEvent(items, exp, player, block.location), mark.authorization)

        // Drop whatever did not fit, as vanilla would have.
        items.forEach { block.world.dropItemNaturally(block.location, it) }
        if (exp.toInt() > 0) {
            block.world.spawn(block.location.toCenterLocation(), ExperienceOrb::class.java) { it.experience = exp.toInt() }
        }
        markNeighbors(block, mark.authorization)
    }

    private fun markNeighbors(block: Block, authorization: DropAuthorization) {
        val now = System.currentTimeMillis()
        if (marks.size > 4096) marks.values.removeIf { it.expiresAt < now }
        val mark = Mark(authorization, now + MARK_LIFETIME_MILLIS)
        directions.forEach { marks[block.getRelative(it).key()] = mark }
    }

    private fun Block.key() = BlockKey(world.uid, x, y, z)
}
