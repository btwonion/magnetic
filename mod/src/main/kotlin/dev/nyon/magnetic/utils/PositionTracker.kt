package dev.nyon.magnetic.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

class PositionTracker {
    private data class Entry(val player: ServerPlayer, val timestamp: Long)

    private val entries = HashMap<BlockPos, Entry>()

    fun recordNeighbors(pos: BlockPos, player: ServerPlayer, level: ServerLevel) {
        record(snapshotNeighbors(pos, level), player)
    }

    fun snapshotNeighbors(pos: BlockPos, level: ServerLevel): List<BlockPos> = Direction.entries.mapNotNull { direction ->
        pos.relative(direction).takeUnless { level.getBlockState(it).isAir }?.immutable()
    }

    fun record(positions: Iterable<BlockPos>, player: ServerPlayer) {
        val now = System.currentTimeMillis()
        for (pos in positions) entries[pos.immutable()] = Entry(player, now)
    }

    fun lookup(pos: BlockPos): ServerPlayer? {
        val entry = entries[pos] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > DEFAULT_TIMEOUT) {
            entries.remove(pos)
            return null
        }
        return entry.player
    }

    fun cleanup() {
        val now = System.currentTimeMillis()
        entries.values.removeIf { now - it.timestamp > DEFAULT_TIMEOUT }
    }

    fun record(pos: BlockPos, player: ServerPlayer, timeout: Long) {
        entries[pos.immutable()] = Entry(player, System.currentTimeMillis() + timeout - DEFAULT_TIMEOUT)
    }

    fun lookupFluid(pos: BlockPos, timeout: Long): ServerPlayer? {
        val entry = entries[pos] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > timeout) {
            entries.remove(pos)
            return null
        }
        return entry.player
    }

    companion object {
        private const val DEFAULT_TIMEOUT = 5000L
    }
}
