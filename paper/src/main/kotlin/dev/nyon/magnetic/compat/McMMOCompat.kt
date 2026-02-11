package dev.nyon.magnetic.compat

import com.gmail.nossr50.events.items.McMMOItemSpawnEvent
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerFishingTreasureEvent
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerShakeEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.event.Event

object McMMOCompat {
    fun listenForEvents() {
        listen<McMMOItemSpawnEvent> {
            val serverPlayer = player ?: return@listen
            val items = mutableListOf(itemStack)
            DropEvent(items, MutableInt(), serverPlayer, location).also(Event::callEvent)

            // Delete items that have been added to the inventory
            if (items.isEmpty()) isCancelled = true
        }

        listen<McMMOPlayerFishingTreasureEvent> {
            val items = listOfNotNull(treasure).toMutableList()
            DropEvent(items, MutableInt(0), player, player.location).also(Event::callEvent)
            treasure = items.firstOrNull()
        }

        listen<McMMOPlayerShakeEvent> {
            val items = mutableListOf(drop)
            DropEvent(items, MutableInt(), player, player.location).also(Event::callEvent)
            drop = items.firstOrNull()
        }
    }
}