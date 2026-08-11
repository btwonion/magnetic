package dev.nyon.magnetic

import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.EntitySnapshot
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.atomic.AtomicBoolean

internal data class ClaimedDrop(
    val location: Location,
    val itemStack: ItemStack,
    val entitySnapshot: EntitySnapshot
)

internal object DropEntityDispatcher {
    fun claim(entity: Item, onClaimed: (ClaimedDrop) -> Unit) {
        entity.scheduler.execute(Main.INSTANCE, {
            if (!entity.isValid) return@execute

            val entitySnapshot = entity.createSnapshot() ?: return@execute
            val claimedDrop = ClaimedDrop(
                entity.location.clone(),
                entity.itemStack.clone(),
                entitySnapshot
            )
            entity.remove()
            onClaimed(claimedDrop)
        }, null, 0L)
    }

    fun dispatch(
        claimedDrop: ClaimedDrop,
        authorization: DropAuthorization?,
        handledDropKey: NamespacedKey
    ) {
        if (authorization == null) {
            restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack), handledDropKey)
            return
        }

        val player = Bukkit.getPlayer(authorization.playerId)
        if (player == null) {
            restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack), handledDropKey)
            return
        }

        val restored = AtomicBoolean()
        val restoreOnFailure = {
            if (restored.compareAndSet(false, true)) {
                restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack), handledDropKey)
            }
        }
        val scheduled = player.scheduler.execute(Main.INSTANCE, {
            if (player.world.uid != claimedDrop.location.world.uid) {
                restoreOnFailure()
                return@execute
            }

            val itemStacks = mutableListOf(claimedDrop.itemStack.clone())
            DropEventDispatcher.callAuthorized(
                DropEvent(itemStacks, MutableInt(), player, claimedDrop.location, handledDropKey),
                authorization
            )
            restoreResidualDrops(claimedDrop, itemStacks, handledDropKey)
        }, restoreOnFailure, 0L)
        if (!scheduled) restoreOnFailure()
    }

    private fun restoreResidualDrops(
        claimedDrop: ClaimedDrop,
        items: List<ItemStack>,
        handledDropKey: NamespacedKey
    ) {
        val residual = items
            .filter { !it.type.isAir && it.amount > 0 }
            .map(ItemStack::clone)
        if (residual.isEmpty()) return

        Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, claimedDrop.location) {
            residual.forEach { restoreDropWithoutEvent(claimedDrop, it, handledDropKey) }
        }
    }

    private fun restoreDropWithoutEvent(
        claimedDrop: ClaimedDrop,
        itemStack: ItemStack,
        handledDropKey: NamespacedKey
    ) {
        val world = claimedDrop.location.world as CraftWorld
        val item = claimedDrop.entitySnapshot.createEntity(world) as Item
        val handle = (item as CraftEntity).handle
        handle.setPos(claimedDrop.location.x, claimedDrop.location.y, claimedDrop.location.z)
        item.itemStack = itemStack
        item.persistentDataContainer.set(handledDropKey, PersistentDataType.BYTE, 1.toByte())

        // Other plugins already observed the original spawn, so restore without firing it twice.
        world.addEntityToWorld(handle, null)
    }
}
