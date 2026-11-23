package dev.nyon.magnetic.compat

import dev.nyon.magnetic.DropAuthorization
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Runs a [DropEvent] for a single stack another plugin is about to drop.
 *
 * Pass [authorization] when the player was already authorized, otherwise the player's conditions are checked.
 * Returns what was not collected, or null when the whole stack was collected.
 */
internal fun collectDrop(
    stack: ItemStack,
    player: Player,
    location: Location,
    authorization: DropAuthorization? = null
): ItemStack? {
    val items = mutableListOf(stack)
    val event = DropEvent(items, MutableInt(), player, location)
    if (authorization == null) DropEventDispatcher.call(event) else DropEventDispatcher.callAuthorized(event, authorization)
    return items.firstOrNull()?.takeUnless(ItemStack::isEmpty)
}

/** Collects a dropped item entity, removing it or shrinking it to the leftover. */
internal fun collectDrop(item: Item, player: Player, authorization: DropAuthorization? = null) {
    val leftover = collectDrop(item.itemStack.clone(), player, item.location, authorization)
    if (leftover == null) item.remove() else item.itemStack = leftover
}
