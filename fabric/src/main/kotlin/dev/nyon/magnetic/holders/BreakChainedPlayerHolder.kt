package dev.nyon.magnetic.holders

import net.minecraft.server.level.ServerPlayer

interface BreakChainedPlayerHolder {
    var initialBreaker: ServerPlayer?
    var rootBroken: Long?
}