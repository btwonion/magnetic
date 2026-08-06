package dev.nyon.magnetic.compat

import de.miraculixx.veinminer.VeinMinerEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt

object VeinminerCompat {
    fun listenForEvents() {
        listen<VeinMinerEvent.VeinminerDropEvent> {
            if (block.type.isIgnored) return@listen

            val mutableInt = MutableInt(exp)
            val itemStacks = items.toMutableList()
            DropEventDispatcher.call(DropEvent(itemStacks, mutableInt, player, block.location))
            exp = mutableInt.toInt()

            // Delete items that have been added to the inventory
            items.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }
    }
}
