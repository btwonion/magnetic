package dev.nyon.magnetic.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

class PositionTracker {
    private val entries = ExpiringMap<BlockPos, ServerPlayer>()

    fun recordNeighbors(pos: BlockPos, player: ServerPlayer, level: ServerLevel) {
        record(snapshotNeighbors(pos, level), player)
    }

    fun snapshotNeighbors(pos: BlockPos, level: ServerLevel): List<BlockPos> = Direction.entries.mapNotNull { direction ->
        pos.relative(direction).takeUnless { level.getBlockState(it).isAir }?.immutable()
    }

    fun record(positions: Iterable<BlockPos>, player: ServerPlayer) {
        for (pos in positions) entries.put(pos.immutable(), player, DEFAULT_TIMEOUT)
    }

    fun lookup(pos: BlockPos): ServerPlayer? = entries[pos]

    fun cleanup() = entries.cleanup()

    fun record(pos: BlockPos, player: ServerPlayer, timeout: Long) {
        entries.put(pos.immutable(), player, timeout)
    }

    @Suppress("UNUSED_PARAMETER")
    fun lookupFluid(pos: BlockPos, timeout: Long): ServerPlayer? = entries[pos]

    companion object {
        private const val DEFAULT_TIMEOUT = 5000L
    }
}
