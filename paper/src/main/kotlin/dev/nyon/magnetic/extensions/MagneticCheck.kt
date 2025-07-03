package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.magneticKey
import org.bukkit.inventory.ItemStack

fun ItemStack.hasMagnetic(): Boolean = hasItemMeta() && itemMeta.enchants.any { it.key.key == magneticKey }