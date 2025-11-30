package dev.nyon.magnetic.config.conditions

import dev.nyon.magnetic.magneticKey
import dev.nyon.magnetic.magneticPermission
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal val conditions: Set<Condition> = setOf(EnchantmentCondition, SneakCondition, PermissionCondition)

sealed interface Condition : Statement {
    override val identifiers: Set<String>
    fun check(player: Player): Boolean
}

object EnchantmentCondition : Condition {
    override val identifiers: Set<String> = setOf("ENCHANTMENT")

    fun ItemStack.hasMagnetic(): Boolean = hasItemMeta() && itemMeta.enchants.any { it.key.key == magneticKey }
    override fun check(player: Player): Boolean {
        return listOf(
            player.inventory.itemInMainHand,
            player.inventory.itemInOffHand
        ).any { it.hasMagnetic() }
    }
}

object SneakCondition : Condition {
    override val identifiers: Set<String> = setOf("SNEAK")

    override fun check(player: Player): Boolean {
        return player.isSneaking
    }
}

object PermissionCondition : Condition {
    override val identifiers: Set<String> = setOf("PERMISSION")

    override fun check(player: Player): Boolean {
        return player.hasPermission(magneticPermission)
    }
}
