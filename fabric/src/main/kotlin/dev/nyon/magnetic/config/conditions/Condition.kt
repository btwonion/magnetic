package dev.nyon.magnetic.config.conditions

import dev.nyon.magnetic.datagen.magneticEffectId
import dev.nyon.magnetic.extensions.PlayerPermissionSupplier
import net.minecraft.commands.Commands
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionCheck
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper

internal val conditions: Set<Condition> = setOf(EnchantmentCondition, SneakCondition, PermissionCondition)

sealed interface Condition : Statement {
    override val identifiers: Set<String>
    fun check(player: ServerPlayer): Boolean
}

object EnchantmentCondition : Condition {
    override val identifiers: Set<String> = setOf("ENCHANTMENT")

    fun ItemStack.hasMagnetic(): Boolean = EnchantmentHelper.hasTag(this, magneticEffectId)
    override fun check(player: ServerPlayer): Boolean {
        return listOf(
            player.mainHandItem, player.offhandItem
        ).any { it.hasMagnetic() }
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
    private val permission = Permission.Atom.create("magnetic.ability.use")

    override fun check(player: ServerPlayer): Boolean {
        return Commands.hasPermission<PlayerPermissionSupplier>(PermissionCheck.Require(permission))
            .test(PlayerPermissionSupplier(player))
    }
}
