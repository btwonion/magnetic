package dev.nyon.magnetic.extensions

import net.minecraft.server.level.ServerPlayer
/*? if >=1.21.11 {*/
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.server.permissions.PermissionSetSupplier
/*?} else if fabric {*/
/*import me.lucko.fabric.api.permissions.v0.Permissions
*//*?} else {*/
/*import net.neoforged.neoforge.server.permission.PermissionAPI
import net.neoforged.neoforge.server.permission.nodes.PermissionNode
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes
*//*?}*/

/*? if >=1.21.11 {*/
class PlayerPermissionSupplier(val player: ServerPlayer) : PermissionSetSupplier {
    override fun permissions(): PermissionSet {
        return player.permissions()
    }
}
/*?} else {*/
/*fun ServerPlayer.hasMagneticPermission(): Boolean =
    /*? if fabric {*/ Permissions.check(this, "magnetic.ability.use", false) /*?} else {*/
    /*PermissionAPI.getPermission(this, MAGNETIC_PERMISSION) *//*?}*/

/*? if neoforge {*/
val MAGNETIC_PERMISSION = PermissionNode(
    "magnetic",
    "ability.use",
    PermissionTypes.BOOLEAN,
    { _, _, _ -> false }
)
/*?}*/
*//*?}*/
