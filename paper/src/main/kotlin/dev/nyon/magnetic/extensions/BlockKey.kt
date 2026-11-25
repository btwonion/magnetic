package dev.nyon.magnetic.extensions

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import java.util.UUID

internal data class BlockKey(val worldId: UUID, val x: Int, val y: Int, val z: Int) {
    fun relative(face: BlockFace) = BlockKey(worldId, x + face.modX, y + face.modY, z + face.modZ)

    fun location(world: World) = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

    fun distanceSquared(other: BlockKey): Long {
        if (worldId != other.worldId) return Long.MAX_VALUE
        val dx = x.toLong() - other.x
        val dy = y.toLong() - other.y
        val dz = z.toLong() - other.z
        return dx * dx + dy * dy + dz * dz
    }
}

internal fun Block.toPositionKey() = BlockKey(world.uid, x, y, z)

internal fun Location.toPositionKey() = BlockKey(world.uid, blockX, blockY, blockZ)

internal val adjacentFaces =
    listOf(BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST)
