package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.ignoredEntities
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

val EntityType.isIgnored: Boolean
    get() {
        return ignoredEntities.contains(key)
    }

fun Entity.failsLongRangeCheck(player: Player): Boolean {
    if (config.ignoredEntitiesRangeMin == -1.0) return false
    return location.distance(player.location) > config.ignoredEntitiesRangeMin
}

fun Player.canAddItem(stack: ItemStack): Boolean {
    val nmsPlayer = (player as CraftPlayer).handle
    if (nmsPlayer.inventory.freeSlot >= 0) return true
    if (nmsPlayer.hasInfiniteMaterials()) return true
    val nmsItem = (stack as CraftItemStack).handle
    if (nmsItem.isDamaged) return false
    if (nmsPlayer.inventory.getSlotWithRemainingSpace(nmsItem) > -1) return true
    return false
}