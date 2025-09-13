@file:Suppress("UNCHECKED_CAST")

package dev.nyon.magnetic

import dev.nyon.magnetic.extensions.failsLongRangeCheck
import dev.nyon.magnetic.extensions.isAllowedToUseMagnetic
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import io.papermc.paper.event.block.PlayerShearBlockEvent
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.Statistic
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerShearEntityEvent

object Listeners {

    @Suppress("unused")
    private val magneticListener = listen<DropEvent> {
        if (!player.isAllowedToUseMagnetic()) return@listen

        if (config.itemsAllowed) {
            items.removeIf { item ->
                val copiedStack = item.clone()
                if (player.inventory.addItem(item).isNotEmpty()) return@removeIf false
                player.incrementStatistic(Statistic.PICKUP, copiedStack.type, copiedStack.amount)
                true
            }
        }
        if (config.expAllowed) {
            player.giveExp(exp.value, true)
            exp.value = 0
        }
    }

    fun listenForBukkitEvents() {
        listen<BlockDropItemEvent> {
            val itemStacks = items.map { it.itemStack }.toMutableList()
            DropEvent(itemStacks, MutableInt(), player).also(Event::callEvent)

            // Delete items that have been added to the inventory
            items.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item.itemStack) }
            }
        }

        listen<BlockBreakEvent> {
            val mutableInt = MutableInt(expToDrop)
            DropEvent(mutableListOf(), mutableInt, player).also(Event::callEvent)
            expToDrop = mutableInt.value
        }

        listen<EntityDeathEvent> {
            if (entityType.isIgnored) return@listen
            if (entity.lastDamageCause.failsLongRangeCheck()) return@listen
            val killer = entity.killer ?: return@listen
            val mutableInt = MutableInt(droppedExp)
            val itemStacks = drops.toMutableList()
            DropEvent(itemStacks, mutableInt, killer).also(Event::callEvent)
            droppedExp = mutableInt.value

            // Delete items that have been added to the inventory
            drops.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }

        listen<PlayerShearBlockEvent> {
            val itemStacks = drops.toMutableList()
            DropEvent(itemStacks, MutableInt(), player).also(Event::callEvent)

            // Delete items that have been added to the inventory
            drops.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }

        listen<PlayerShearEntityEvent> {
            val itemStacks = drops.toMutableList()
            DropEvent(itemStacks, MutableInt(), player).also(Event::callEvent)

            // Delete items that have been added to the inventory
            drops.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }

        listen<PlayerFishEvent> {
            val mutableInt = MutableInt(expToDrop)
            DropEvent(mutableListOf(), mutableInt, player).also(Event::callEvent)
            expToDrop = mutableInt.value
        }
    }

    fun listenForModEvents() { // Add Veinminer integration
        if (Bukkit.getPluginManager().isPluginEnabled("Veinminer")) {
            listen<de.miraculixx.veinminer.VeinMinerEvent.VeinminerDropEvent> {
                val mutableInt = MutableInt(exp)
                val itemStacks = items.toMutableList()
                DropEvent(itemStacks, mutableInt, player).also(Event::callEvent)
                exp = mutableInt.value

                // Delete items that have been added to the inventory
                items.removeIf { item ->
                    itemStacks.none { stack -> stack.isSimilar(item) }
                }
            }
        }
    }
}
