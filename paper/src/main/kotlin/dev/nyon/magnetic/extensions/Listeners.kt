package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.Plugin
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

abstract class SingleListener<T : Event> : Listener {
    abstract fun onEvent(event: T)
}

inline fun <reified T : Event> listen(crossinline eventCallback: T.() -> Unit) {
    val listener = object : SingleListener<T>() {
        override fun onEvent(event: T) = eventCallback(event)
    }

    Bukkit.getPluginManager().registerEvent(
        T::class.java,
        listener,
        EventPriority.NORMAL,
        { _, event -> listener.onEvent(event as T) },
        Plugin
    )
}