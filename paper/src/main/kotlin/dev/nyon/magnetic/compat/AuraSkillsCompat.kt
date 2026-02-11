package dev.nyon.magnetic.compat

import dev.aurelium.auraskills.api.event.loot.LootDropEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.event.Event

object AuraSkillsCompat {

    fun listenForEvents() {
        listen<LootDropEvent> {
            val items = mutableListOf(item)
            DropEvent(items, MutableInt(), player, location).also(Event::callEvent)

            // Delete items that have been added to the inventory
            if (items.isEmpty()) isCancelled = true
        }
    }
}