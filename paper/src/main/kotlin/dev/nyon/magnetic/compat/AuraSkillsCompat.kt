package dev.nyon.magnetic.compat

import dev.aurelium.auraskills.api.event.loot.LootDropEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt

object AuraSkillsCompat {

    fun listenForEvents() {
        listen<LootDropEvent> {
            val items = mutableListOf(item)
            DropEventDispatcher.call(DropEvent(items, MutableInt(), player, location))

            // Delete items that have been added to the inventory
            if (items.isEmpty()) isCancelled = true
        }
    }
}
