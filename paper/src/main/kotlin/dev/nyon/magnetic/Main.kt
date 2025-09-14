package dev.nyon.magnetic

import dev.nyon.magnetic.config.reloadIgnoredEntities
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.moveTo

val magneticKey = NamespacedKey("magnetic", "magnetic")
const val magneticPermission = "magnetic.ability.use"
val configPath: Path = Bukkit.getPluginsFolder().toPath().resolve("magnetic/magnetic.json")

class Main : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: Main; private set
    }

    override fun onLoad() {
        INSTANCE = this
        moveConfigToNewPath(configPath)
        reloadIgnoredEntities()
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
