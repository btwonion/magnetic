package dev.nyon.magnetic.compat

import de.miraculixx.veinminer.VeinMinerEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.event.Event

object VeinminerCompat {
    fun listenForEvents() {
        listen<VeinMinerEvent.VeinminerDropEvent> {
            val mutableInt = MutableInt(exp)
            val itemStacks = items.toMutableList()
            DropEvent(itemStacks, mutableInt, player, block.location).also(Event::callEvent)
            exp = mutableInt.value

            // Delete items that have been added to the inventory
            items.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }
    }
}