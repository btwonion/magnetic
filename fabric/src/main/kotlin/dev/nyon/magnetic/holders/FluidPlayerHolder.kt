package dev.nyon.magnetic.holders

import net.minecraft.server.level.ServerPlayer

interface FluidPlayerHolder {
    var player: ServerPlayer?
    var placedAt: Long?
}