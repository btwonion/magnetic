@file:Suppress("UNCHECKED_CAST")

package dev.nyon.magnetic

import dev.nyon.magnetic.extensions.hasMagnetic
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
import org.bukkit.inventory.ItemStack

object Listeners {

    @Suppress("unused")
    private val magneticListener = listen<DropEvent> {
        if (config.needEnchantment && listOf(
                player.inventory.itemInMainHand, player.inventory.itemInOffHand
            ).none { it.hasMagnetic() }
        ) return@listen
        if (config.needSneak && !player.isSneaking) return@listen

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (player.inventory.addItem(item).isNotEmpty()) return@removeIf false
                player.incrementStatistic(Statistic.PICKUP, item.type, item.amount)
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
            DropEvent(items as MutableList<ItemStack>, MutableInt(), player).also(Event::callEvent)
        }

        listen<BlockBreakEvent> {
            val mutableInt = MutableInt(expToDrop)
            DropEvent(mutableListOf(), mutableInt, player).also(Event::callEvent)
            expToDrop = mutableInt.value
        }

        listen<EntityDeathEvent> {
            val killer = entity.killer ?: return@listen
            val mutableInt = MutableInt(droppedExp)
            DropEvent(drops as MutableList<ItemStack>, mutableInt, killer).also(Event::callEvent)
            droppedExp = mutableInt.value
        }

        listen<PlayerShearBlockEvent> {
            DropEvent(drops as MutableList<ItemStack>, MutableInt(), player).also(Event::callEvent)
        }

        listen<PlayerShearEntityEvent> {
            DropEvent(drops as MutableList<ItemStack>, MutableInt(), player).also(Event::callEvent)
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
                DropEvent(items, mutableInt, player).also(Event::callEvent)
                exp = mutableInt.value
            }
        }
    }
}