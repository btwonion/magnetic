package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.extensions.failsLongRangeCheck
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerShearEntityEvent

@Suppress("unused")
object ItemListeners {

    private val entityDeathEvent = listen<EntityDeathEvent> {
        if (entityType.isIgnored) return@listen
        val killer = entity.killer ?: return@listen
        if (entity.failsLongRangeCheck(killer)) return@listen
        if (entityType == EntityType.PLAYER && Bukkit.getPluginManager()
                .isPluginEnabled("GravesX")
        ) return@listen // Disable player death drops in favor of GravesX
        val mutableInt = MutableInt(droppedExp)
        val itemStacks = drops.toMutableList()
        DropEventDispatcher.call(DropEvent(itemStacks, mutableInt, killer, entity.location))
        droppedExp = mutableInt.toInt()

        // Delete items that have been added to the inventory
        drops.removeIf { item ->
            itemStacks.none { stack -> stack.isSimilar(item) }
        }
    }

    private val playerShearEvent = listen<PlayerShearEntityEvent> {
        val itemStacks = drops.toMutableList()
        DropEventDispatcher.call(DropEvent(itemStacks, MutableInt(), player, entity.location))

        // Delete items that have been added to the inventory
        drops.removeIf { item ->
            itemStacks.none { stack -> stack.isSimilar(item) }
        }
    }

    private val playerFishEvent = listen<PlayerFishEvent> {
        if (caught == null) return@listen
        val mutableInt = MutableInt(expToDrop)
        DropEventDispatcher.call(DropEvent(mutableListOf(), mutableInt, player, caught!!.location))
        expToDrop = mutableInt.toInt()
    }
}
