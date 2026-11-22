package dev.nyon.magnetic.compat

import de.miraculixx.veinminer.VeinMinerEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit

object VeinminerCompat {
    private const val PLUGIN_NAME = "Veinminer"
    private const val DROP_EVENT_CLASS = "de.miraculixx.veinminer.VeinMinerEvent\$VeinminerDropEvent"

    /**
     * Registers the drop listener only when the enabled "Veinminer" plugin is Miraculixx's Veinminer.
     * Other plugins share that name, so the plugin name alone is not enough to safely reference its classes.
     */
    fun listenForEventsIfPresent() {
        if (!Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME)) return

        if (!isDropEventAvailable()) {
            Main.INSTANCE.logger.warning(
                "A plugin named '$PLUGIN_NAME' is enabled, but it is not Miraculixx's Veinminer " +
                    "($DROP_EVENT_CLASS not found). Veinminer compatibility is disabled."
            )
            return
        }

        try {
            listenForEvents()
        } catch (e: LinkageError) {
            Main.INSTANCE.logger.warning(
                "Failed to hook into Veinminer, compatibility is disabled: ${e.javaClass.simpleName}: ${e.message}"
            )
        }
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
