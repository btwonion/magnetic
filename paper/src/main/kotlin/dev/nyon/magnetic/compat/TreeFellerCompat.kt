package dev.nyon.magnetic.compat

import com.thizthizzydizzy.treefeller.compat.PluginCompatibility
import dev.nyon.magnetic.Main
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
    fun register() {
        TreeFellerCompatibilities.addPluginCompatibility(MagneticTreeFellerCompatibility())
    }
}

private class MagneticTreeFellerCompatibility : PluginCompatibility() {
    override fun getPluginName(): String = "Magnetic"

    override fun isInstalled(): Boolean = Main.INSTANCE.isEnabled

    // Tree Feller only assigns option values while loading its config, which happens before Magnetic registers.
    // Treat an unset option as enabled; "CompatibilityMagnetic: false" in Tree Feller's config still disables it.
    override fun isEnabled(): Boolean = enabled.value != false

    override fun dropItem(player: Player?, item: Item) {
        if (player == null || !item.isValid || item.itemStack.isEmpty) return

        // Delete items that have been added to the inventory, keep the leftover otherwise
        collectDrop(item, player)
    }
}
