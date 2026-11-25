package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.ignoredBlocks
import dev.nyon.magnetic.config.ignoredEntities
import org.bukkit.Material
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

val EntityType.isIgnored: Boolean
    get() = key in ignoredEntities

val Material.isIgnored: Boolean
    get() = key in ignoredBlocks

fun Entity.failsLongRangeCheck(player: Player): Boolean {
    if (config.ignoredEntitiesRangeMin == -1.0) return false
    return location.distance(player.location) > config.ignoredEntitiesRangeMin
}

fun Player.canAddItem(stack: ItemStack): Boolean {
    val nmsPlayer = (player as CraftPlayer).handle
    if (nmsPlayer.inventory.freeSlot >= 0) return true
    if (nmsPlayer.hasInfiniteMaterials()) return true
    val nmsItem = (stack as CraftItemStack).handle
    return !nmsItem.isDamaged && nmsPlayer.inventory.getSlotWithRemainingSpace(nmsItem) > -1
}
