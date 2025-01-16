package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config
import dev.nyon.magnetic.magneticKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun ItemStack.hasTelekinesis(): Boolean = hasItemMeta() && itemMeta.enchants.any { it.key.key == magneticKey }

fun Player.isEligible(): Boolean {
    val enchantmentValid = !config.needEnchantment || config.needEnchantment && listOf(
        inventory.itemInMainHand,
        inventory.itemInMainHand
    ).any { it.hasTelekinesis() }
    val crouchValid = !config.needSneak || config.needSneak && isSneaking
    return enchantmentValid && crouchValid
}