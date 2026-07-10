package dev.nyon.magnetic.extensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.server.permissions.PermissionSetSupplier

class PlayerPermissionSupplier(val player: ServerPlayer) : PermissionSetSupplier {
    override fun permissions(): PermissionSet {
        return player.permissions()
    }
}