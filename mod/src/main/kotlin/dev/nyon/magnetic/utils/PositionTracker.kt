package dev.nyon.magnetic.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

class PositionTracker {
    private val entries = ExpiringMap<BlockPos, ServerPlayer>()

    fun recordNeighbors(pos: BlockPos, player: ServerPlayer, level: ServerLevel) {
        for (direction in Direction.entries) {
            val neighbor = pos.relative(direction)
            if (!level.getBlockState(neighbor).isAir) {
                entries.put(neighbor.immutable(), player, DEFAULT_TIMEOUT)
            }
        }
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
