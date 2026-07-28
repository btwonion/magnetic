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
private const val MISSING_PERMISSION_API_MESSAGE =
    "[magnetic] Your condition chain includes a PERMISSION condition, but fabric-permissions-api is not present. Please install it or remove the PERMISSION condition."

private val fabricPermissionCheckResult by lazy {
    runCatching {
        Class.forName("me.lucko.fabric.api.permissions.v0.Permissions").getMethod(
            "check",
            net.minecraft.world.entity.Entity::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType!!
        )
    }
}

internal fun getFabricPermissionCheck() = fabricPermissionCheckResult.getOrElse { exception ->
    throw IllegalStateException(MISSING_PERMISSION_API_MESSAGE, exception)
}

fun ServerPlayer.hasMagneticPermission(): Boolean {
    return try {
        getFabricPermissionCheck().invoke(null, this, "magnetic.ability.use", false) as Boolean
    } catch (exception: ReflectiveOperationException) {
        throw IllegalStateException("[magnetic] Failed to check the magnetic permission.", exception)
    }
}
/*?} else {*/
/*fun ServerPlayer.hasMagneticPermission(): Boolean {
    return hasNeoForgePermission(this)
}
*//*?}*/
*//*?}*/
