package dev.nyon.magnetic.config.conditions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.datagen.magneticEffectId
/*? if >=1.21.11 {*/
import dev.nyon.magnetic.extensions.PlayerPermissionSupplier
//? if neoforge
//import dev.nyon.magnetic.extensions.hasNeoForgePermission
/*?} else {*/
/*import dev.nyon.magnetic.extensions.hasMagneticPermission
*//*?}*/
import net.minecraft.commands.Commands
import net.minecraft.server.level.ServerPlayer
/*? if >=1.21.11 {*/
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionCheck
/*?}*/
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper

internal val conditions: Set<Condition> = setOf(EnchantmentCondition, SneakCondition, PermissionCondition)

sealed interface Condition {
    val identifiers: Set<String>
    fun check(player: ServerPlayer): Boolean
}

object EnchantmentCondition : Condition {
    override val identifiers: Set<String> = setOf("ENCHANTMENT")

    fun ItemStack.hasMagnetic(): Boolean = EnchantmentHelper.hasTag(this, magneticEffectId)
    override fun check(player: ServerPlayer): Boolean {
        return listOf(
            player.mainHandItem, player.offhandItem
        ).any { it.hasMagnetic() || config.buckets.enabled && it.`is`(Items.BUCKET) }
    }
}

object SneakCondition : Condition {
    override val identifiers: Set<String> = setOf("SNEAK")

    override fun check(player: ServerPlayer): Boolean {
        return player.isCrouching
    }
}

object PermissionCondition : Condition {
    override val identifiers: Set<String> = setOf("PERMISSION")
    //? if >=1.21.11
    private val permission = Permission.Atom.create("magnetic.ability.use")

    override fun check(player: ServerPlayer): Boolean {
        var result: Boolean
        //? if >=1.21.11
        result = Commands.hasPermission<PlayerPermissionSupplier>(PermissionCheck.Require(permission)).test(PlayerPermissionSupplier(player))
        //? if >=1.21.11 && neoforge
        //result = result || hasNeoForgePermission(player)
        //? if <1.21.11
        //result = player.hasMagneticPermission()

        return result
    }
}
