package dev.nyon.magnetic.compat

import de.miraculixx.veinminer.VeinMinerEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt

object VeinminerCompat {
    private const val DROP_EVENT_CLASS = "de.miraculixx.veinminer.VeinMinerEvent\$VeinminerDropEvent"

    /**
     * Registers the drop listener only when Miraculixx's Veinminer is on the classpath.
     * Other plugins may share the "Veinminer" name, so the event class is checked instead of the plugin name.
     */
    fun listenForEventsIfPresent() {
        if (isDropEventAvailable()) listenForEvents()
    }

    private fun isDropEventAvailable(): Boolean {
        return try {
            Class.forName(DROP_EVENT_CLASS, false, VeinminerCompat::class.java.classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    private fun listenForEvents() {
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
