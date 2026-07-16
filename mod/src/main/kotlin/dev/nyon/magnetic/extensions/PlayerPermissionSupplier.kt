package dev.nyon.magnetic.extensions

import net.minecraft.server.level.ServerPlayer
/*? if >=1.21.11 {*/
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.server.permissions.PermissionSetSupplier
/*?} else if fabric {*/
/*import net.minecraft.world.entity.Entity
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
    /*? if fabric {*/ fabricPermissionCheck?.let { check ->
        runCatching { check.invoke(null, this, "magnetic.ability.use", false) as Boolean }.getOrDefault(false)
    } ?: false /*?} else {*/
    /*PermissionAPI.getPermission(this, MAGNETIC_PERMISSION) *//*?}*/

/*? if fabric {*/
private val fabricPermissionCheck by lazy {
    runCatching {
        // Keep the compile-only integration out of the runtime linkage graph.
        Class.forName("me.lucko.fabric.api.permissions.v0.Permissions").getMethod(
            "check",
            Entity::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType!!
        )
    }.getOrNull()
}
/*?}*/

/*? if neoforge {*/
val MAGNETIC_PERMISSION = PermissionNode(
    "magnetic",
    "ability.use",
    PermissionTypes.BOOLEAN,
    { _, _, _ -> false }
)
/*?}*/
*//*?}*/
