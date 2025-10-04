package dev.nyon.magnetic

import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

data class DropEvent(val items: MutableList<ItemStack>, val exp: MutableInt, val player: Player, val pos: Location) : Event() {
    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic
        @Suppress("unused")
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }
}