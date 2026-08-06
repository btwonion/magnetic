package dev.nyon.magnetic.compat

import com.gmail.nossr50.events.items.McMMOItemSpawnEvent
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerFishingTreasureEvent
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerShakeEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt

object McMMOCompat {
    fun listenForEvents() {
        listen<McMMOItemSpawnEvent> {
            val serverPlayer = player ?: return@listen
            val items = mutableListOf(itemStack)
            DropEventDispatcher.call(DropEvent(items, MutableInt(), serverPlayer, location))

            // Delete items that have been added to the inventory
            if (items.isEmpty()) isCancelled = true
        }

        listen<McMMOPlayerFishingTreasureEvent> {
            val items = listOfNotNull(treasure).toMutableList()
            DropEventDispatcher.call(DropEvent(items, MutableInt(0), player, player.location))
            treasure = items.firstOrNull()
        }

        listen<McMMOPlayerShakeEvent> {
            val items = mutableListOf(drop)
            DropEventDispatcher.call(DropEvent(items, MutableInt(), player, player.location))
            drop = items.firstOrNull()
        }
    }
}
