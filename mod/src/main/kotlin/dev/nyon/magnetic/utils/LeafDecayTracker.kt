package dev.nyon.magnetic.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.LeavesBlock
import java.util.ArrayDeque
import java.util.UUID

class LeafDecayTracker {
    private data class Entry(
        val playerUuid: UUID,
        val expiresAt: Long,
        val generation: Long
    )

    private val entries = HashMap<BlockPos, Entry>()
    private var generation = 0L

    fun record(pos: BlockPos, playerUuid: UUID, timeout: Long) {
        record(listOf(pos), playerUuid, timeout)
    }

    fun record(positions: Iterable<BlockPos>, playerUuid: UUID, timeout: Long) {
        val now = System.currentTimeMillis()
        val positiveTimeout = timeout.coerceAtLeast(0)
        val expiresAt = if (positiveTimeout > Long.MAX_VALUE - now) Long.MAX_VALUE else now + positiveTimeout
        val entry = Entry(
            playerUuid,
            expiresAt,
            ++generation
        )
        for (pos in positions) entries[pos.immutable()] = entry
    }

    fun recordDecayCandidates(sourcePos: BlockPos, playerUuid: UUID, timeout: Long, level: ServerLevel) {
        val candidates = Direction.entries
            .map(sourcePos::relative)
            .filter { distanceWillIncrease(it, level) }
        if (candidates.isNotEmpty()) record(candidates, playerUuid, timeout)
    }

    fun propagate(pos: BlockPos, level: ServerLevel) {
        val source = getActive(pos) ?: return
        for (direction in Direction.entries) {
            val neighbor = pos.relative(direction)
            if (!distanceWillIncrease(neighbor, level)) continue

            val current = getActive(neighbor)
            if (current == null || current.generation < source.generation) {
                entries[neighbor.immutable()] = source
            }
        }
    }

    fun take(pos: BlockPos): UUID? = getActive(pos)?.playerUuid.also {
        entries.remove(pos)
    }

    fun cleanup() {
        val now = System.currentTimeMillis()
        entries.values.removeIf { it.expiresAt <= now }
    }

    private fun getActive(pos: BlockPos): Entry? {
        val entry = entries[pos] ?: return null
        if (entry.expiresAt <= System.currentTimeMillis()) {
            entries.remove(pos)
            return null
        }
        return entry
    }

    private fun distanceWillIncrease(pos: BlockPos, level: ServerLevel): Boolean {
        val state = level.getBlockState(pos)
        if (!state.`is`(BlockTags.LEAVES) || !state.hasProperty(LeavesBlock.DISTANCE)) return false

        val currentDistance = state.getValue(LeavesBlock.DISTANCE)
        var updatedDistance = LeavesBlock.DECAY_DISTANCE
        for (direction in Direction.entries) {
            val neighborDistance = LeavesBlock.getOptionalDistanceAt(level.getBlockState(pos.relative(direction)))
                .orElse(LeavesBlock.DECAY_DISTANCE)
            updatedDistance = minOf(updatedDistance, neighborDistance + 1)
            if (updatedDistance == 1) break
        }
        return updatedDistance > currentDistance && !hasLogSupport(pos, level)
    }

    private fun hasLogSupport(pos: BlockPos, level: ServerLevel): Boolean {
        val visited = hashSetOf(pos.immutable())
        val queue = ArrayDeque<Pair<BlockPos, Int>>()
        queue.add(pos.immutable() to 0)

        while (queue.isNotEmpty()) {
            val (current, depth) = queue.removeFirst()
            if (depth >= LeavesBlock.DECAY_DISTANCE - 1) continue

            for (direction in Direction.entries) {
                val neighbor = current.relative(direction)
                if (!visited.add(neighbor.immutable())) continue

                val state = level.getBlockState(neighbor)
                if (state.`is`(BlockTags.LOGS)) return true
                if (state.`is`(BlockTags.LEAVES)) {
                    queue.add(neighbor.immutable() to depth + 1)
                }
            }
        }
        return false
    }
}
