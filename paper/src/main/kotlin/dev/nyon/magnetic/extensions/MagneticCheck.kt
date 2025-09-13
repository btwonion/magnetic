package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.ignoredEntities
import dev.nyon.magnetic.magneticKey
import dev.nyon.magnetic.magneticPermission
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

fun ItemStack.hasMagnetic(): Boolean = hasItemMeta() && itemMeta.enchants.any { it.key.key == magneticKey }

fun Player.isAllowedToUseMagnetic(): Boolean {
    if (config.enchantmentRequired && listOf(
            inventory.itemInMainHand,
            inventory.itemInOffHand
        ).none { it.hasMagnetic() }
    ) return false
    if (config.sneakRequired && !isSneaking) return false
    if (config.permissionRequired && !hasPermission(magneticPermission)) return false

    return true
}

val EntityType.isIgnored: Boolean
    get() {
        return ignoredEntities.contains(key)
    }

fun EntityDamageEvent?.failsLongRangeCheck(): Boolean {
    if (this == null) return true
    return config.ignoreRangedWeapons && cause == EntityDamageEvent.DamageCause.PROJECTILE
}