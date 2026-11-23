package dev.nyon.magnetic.compat

import de.miraculixx.veinminer.VeinMinerEvent
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.event.EventPriority

object VeinminerCompat {
    fun listenForEvents() {
        // Runs late so plugins that rewrite Veinminer's drops first, such as auto-smelt plugins, are respected.
        listen<VeinMinerEvent.VeinminerDropEvent>(EventPriority.HIGHEST) {
            if (block.type.isIgnored) return@listen
            val authorization = DropEventDispatcher.authorize(player) ?: return@listen

            // Let auto-smelt datapacks smelt Veinminer's spawned items before they are collected.
            val deferItems = SmeltingDatapackCompat.defersDrops(player)
            if (deferItems) SmeltingDatapackCompat.deferVeinminerDrops(items, authorization)

            val mutableInt = MutableInt(exp)
            val itemStacks = if (deferItems) mutableListOf() else items.toMutableList()
            DropEventDispatcher.callAuthorized(DropEvent(itemStacks, mutableInt, player, block.location), authorization)
            exp = mutableInt.toInt()
            if (deferItems) return@listen

            // Keep only what was not collected. Veinminer drops its own list instance, so update it in place.
            items.clear()
            items.addAll(itemStacks)
        }
    }
}
