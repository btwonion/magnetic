package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.compat.SmeltingDatapackCompat
import dev.nyon.magnetic.compat.TreeCapitatorCompat
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import io.papermc.paper.event.block.PlayerShearBlockEvent
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.player.PlayerHarvestBlockEvent

@Suppress("unused")
object BlockListeners {

    // Runs late so plugins that rewrite the drops first, such as auto-smelt plugins, are respected.
    private val blockDropItemEvent =
        listen<BlockDropItemEvent>(EventPriority.HIGHEST) {
            if (isCancelled || blockState.type.isIgnored) return@listen
            val authorization = DropEventDispatcher.authorize(player) ?: return@listen

            val magneticItems = items.filterNot(TreeCapitatorCompat::isTriggerItem)
            val itemStacks = magneticItems.map { it.itemStack }.toMutableList()

            // Let auto-smelt datapacks smelt the spawned items before they are collected.
            if (SmeltingDatapackCompat.defersDrops(player)) {
                items.removeAll(magneticItems.toSet())
                itemStacks.forEach { stack ->
                    SmeltingDatapackCompat.collectAfterDatapack(
                        player.world.dropItemNaturally(block.location, stack),
                        authorization
                    )
                }
                return@listen
            }

            DropEventDispatcher.callAuthorized(
                DropEvent(itemStacks, MutableInt(), player, block.location),
                authorization
            )

            items.removeAll(magneticItems.toSet())
            itemStacks.forEach { stack ->
                player.world.dropItemNaturally(block.location, stack)
            }
        }

    private val harvestBlockEvent = listen<PlayerHarvestBlockEvent> {
        if (harvestedBlock.type.isIgnored) return@listen
        DropEventDispatcher.collect(itemsHarvested, player, harvestedBlock.location)
    }

    private val playerShearBlockEvent = listen<PlayerShearBlockEvent> {
        if (block.type.isIgnored) return@listen
        DropEventDispatcher.collect(drops, player, block.location)
    }

    private val blockBreakEvent = listen<BlockBreakEvent> {
        if (block.type.isIgnored) return@listen
        val mutableInt = MutableInt(expToDrop)
        DropEventDispatcher.call(DropEvent(mutableListOf(), mutableInt, player, block.location))
        expToDrop = mutableInt.toInt()
    }
}
