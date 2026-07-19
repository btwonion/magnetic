package dev.nyon.magnetic.extensions

import net.minecraft.server.level.ServerPlayer

/*? if >=1.21.11 {*/
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.server.permissions.PermissionSetSupplier
/*?}*/
/*? if neoforge {*/
/*import net.neoforged.neoforge.server.permission.PermissionAPI
import net.neoforged.neoforge.server.permission.nodes.PermissionNode
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes

val MAGNETIC_PERMISSION = PermissionNode(
    "magnetic",
    "ability.use",
    PermissionTypes.BOOLEAN,
    { _, _, _ -> false }
)

fun hasNeoForgePermission(player: ServerPlayer): Boolean {
    return PermissionAPI.getPermission(player, MAGNETIC_PERMISSION)
}
*//*?}*/

/*? if >=1.21.11 {*/
class PlayerPermissionSupplier(val player: ServerPlayer) : PermissionSetSupplier {
    override fun permissions(): PermissionSet {
        return player.permissions()
    }
}
/*?} else {*/
/*/*? if fabric {*/
fun ServerPlayer.hasMagneticPermission(): Boolean {
    return me.lucko.fabric.api.permissions.v0.Permissions.check(this, "magnetic.ability.use", false)
}
/*?} else {*/
/*fun ServerPlayer.hasMagneticPermission(): Boolean {
    return hasNeoForgePermission(this)
}
*//*?}*/
*//*?}*/
