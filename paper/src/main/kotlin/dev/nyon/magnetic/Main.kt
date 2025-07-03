package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import dev.nyon.magnetic.config as internalConfig

val magneticKey = NamespacedKey("magnetic", "magnetic")

class Main : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: Main; private set
    }

    override fun onLoad() {
        INSTANCE = this
        config(Bukkit.getPluginsFolder().toPath().resolve("magnetic.json"), 1, Config()) { _, _, _ -> null }
        internalConfig = loadConfig()
    }

    override fun onEnable() {
        Listeners.listenForBukkitEvents()
        Listeners.listenForModEvents()
    }
}

val Plugin by lazy { Main.INSTANCE }