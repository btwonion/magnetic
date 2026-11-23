package dev.nyon.magnetic.compat

import dev.aurelium.auraskills.api.event.loot.LootDropEvent
import dev.nyon.magnetic.extensions.listen

object AuraSkillsCompat {

    fun listenForEvents() {
        listen<LootDropEvent> {
            // Delete items that have been added to the inventory
            if (collectDrop(item, player, location) == null) isCancelled = true
        }
    }
}
