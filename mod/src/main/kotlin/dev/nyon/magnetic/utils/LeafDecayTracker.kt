package dev.nyon.magnetic.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
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

    fun propagate(pos: BlockPos, level: ServerLevel) {
        val source = getActive(pos) ?: return
        for (direction in Direction.entries) {
            val neighbor = pos.relative(direction)
            if (!level.getBlockState(neighbor).`is`(BlockTags.LEAVES)) continue

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
}
