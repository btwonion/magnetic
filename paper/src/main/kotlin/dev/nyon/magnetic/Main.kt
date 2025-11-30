package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.magnetic.config.Command
import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.MiniMessageTranslator
import dev.nyon.magnetic.config.migrate
import dev.nyon.magnetic.config.reloadIgnoredEntities
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.translation.GlobalTranslator
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
        val configPath = Bukkit.getPluginsFolder().toPath().resolve("magnetic/magnetic.json")
        moveConfigToNewPath(configPath)
        config(dev.nyon.magnetic.configPath, 4, Config()) { _, element, version ->
            migrate(element, version)
        }
        reloadIgnoredEntities()
        MiniMessageTranslator.loadTranslations()
        GlobalTranslator.translator().addSource(MiniMessageTranslator)
        lifecycleManager.registerEventHandler(
            LifecycleEvents.COMMANDS
        ) { event ->
            event.registrar().register(Command.root.build())
        }
        Command.root
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
