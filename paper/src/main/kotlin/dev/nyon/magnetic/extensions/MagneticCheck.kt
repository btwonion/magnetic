package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config
import dev.nyon.magnetic.magneticKey
import dev.nyon.magnetic.magneticPermission
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun ItemStack.hasMagnetic(): Boolean = hasItemMeta() && itemMeta.enchants.any { it.key.key == magneticKey }

fun Player.isAllowedToUseMagnetic(): Boolean {
    if (config.needEnchantment && listOf(
            inventory.itemInMainHand,
            inventory.itemInOffHand
        ).none { it.hasMagnetic() }
    ) return false
    if (config.needSneak && !isSneaking) return false
    if (config.needPermission && !hasPermission(magneticPermission)) return false

    return true
}