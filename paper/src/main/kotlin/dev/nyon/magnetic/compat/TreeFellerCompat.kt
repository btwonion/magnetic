package dev.nyon.magnetic.compat

import com.thizthizzydizzy.treefeller.compat.PluginCompatibility
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.Main
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import com.thizthizzydizzy.treefeller.compat.TreeFellerCompat as TreeFellerCompatibilities

/**
 * Integration for ThizThizzyDizzy's Tree Feller.
 *
 * Tree Feller spawns the drops of felled trees itself, outside of Bukkit's
 * block drop events, and hands every spawned item to its registered plugin
 * compatibilities. Magnetic registers one of those to collect the items.
 */
object TreeFellerCompat {
    private const val PLUGIN_NAME = "TreeFeller"
    private const val COMPATIBILITY_API_CLASS = "com.thizthizzydizzy.treefeller.compat.TreeFellerCompat"

    fun registerIfPresent() {
        if (!Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME) || !isCompatibilityApiAvailable()) return
        TreeFellerCompatibilities.addPluginCompatibility(MagneticTreeFellerCompatibility())
    }

    // Other plugins may also be named "TreeFeller"; only hook into the one exposing this API.
    private fun isCompatibilityApiAvailable(): Boolean {
        return try {
            Class.forName(COMPATIBILITY_API_CLASS, false, TreeFellerCompat::class.java.classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }
}

private class MagneticTreeFellerCompatibility : PluginCompatibility() {
    override fun getPluginName(): String = "Magnetic"

    override fun isInstalled(): Boolean = Main.INSTANCE.isEnabled

    // Tree Feller only assigns option values while loading its config, which happens before Magnetic registers.
    // Treat an unset option as enabled; "CompatibilityMagnetic: false" in Tree Feller's config still disables it.
    override fun isEnabled(): Boolean = enabled.value != false

    override fun dropItem(player: Player?, item: Item) {
        if (player == null || !item.isValid) return

        val stack = item.itemStack
        if (stack.isEmpty) return

        val items = mutableListOf(stack)
        DropEventDispatcher.call(DropEvent(items, MutableInt(), player, item.location))

        // Delete items that have been added to the inventory, keep the leftover otherwise
        val leftover = items.firstOrNull()
        if (leftover == null || leftover.isEmpty) item.remove()
        else item.itemStack = leftover
    }
}
