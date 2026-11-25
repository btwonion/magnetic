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
        droppedExp = DropEventDispatcher.collect(drops, killer, entity.location, droppedExp)
    }

    private val playerShearEvent = listen<PlayerShearEntityEvent> {
        DropEventDispatcher.collect(drops, player, entity.location)
    }

    private val playerFishEvent = listen<PlayerFishEvent> {
        val caughtEntity = caught ?: return@listen
        val mutableInt = MutableInt(expToDrop)
        DropEventDispatcher.call(DropEvent(mutableListOf(), mutableInt, player, caughtEntity.location))
        expToDrop = mutableInt.toInt()
    }
}
