package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import dev.nyon.magnetic.config as internalConfig

val magneticKey = NamespacedKey("magnetic", "magnetic")

class Main : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: Main; private set
    }

    override fun onLoad() {
        INSTANCE = this
        val configPath = Bukkit.getPluginsFolder().toPath().resolve("magnetic/magnetic.json")
        moveConfigToNewPath(configPath)
        config(configPath, 1, Config()) { _, _, _ -> null }
        internalConfig = loadConfig()
    }

    override fun onEnable() {
        Listeners.listenForBukkitEvents()
        Listeners.listenForModEvents()
    }

    private fun moveConfigToNewPath(newPath: Path) {
        val oldPath = Bukkit.getPluginsFolder().toPath().resolve("magnetic.json")
        if (oldPath.exists()) oldPath.moveTo(newPath.createParentDirectories(), overwrite = true)
    }
}

val Plugin by lazy { Main.INSTANCE }
