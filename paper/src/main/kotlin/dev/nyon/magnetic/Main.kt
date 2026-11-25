package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.magnetic.common.config.CONFIG_VERSION
import dev.nyon.magnetic.common.config.Config
import dev.nyon.magnetic.common.config.migrate
import dev.nyon.magnetic.compat.PluginCompats
import dev.nyon.magnetic.config.Command
import dev.nyon.magnetic.config.MiniMessageTranslator
import dev.nyon.magnetic.listeners.BlockListeners
import dev.nyon.magnetic.listeners.DropEventListener
import dev.nyon.magnetic.listeners.LeafDecayListeners
import dev.nyon.magnetic.listeners.ItemListeners
import dev.nyon.magnetic.listeners.FluidListeners
import dev.nyon.magnetic.listeners.SupportBreakListeners
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

class Main : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: Main; private set
    }

    override fun onLoad() {
        INSTANCE = this
        val configPath = dataFolder.toPath().resolve("magnetic.json")
        moveConfigToNewPath(configPath)
        config(configPath, CONFIG_VERSION, Config()) { _, element, version ->
            migrate(element, version)
        }
        registerTranslations()
        registerCommand()
    }

    override fun onEnable() {
        DropEventListener
        ItemListeners
        BlockListeners
        LeafDecayListeners
        FluidListeners
        SupportBreakListeners

        PluginCompats.registerAvailable()
    }

    private fun moveConfigToNewPath(newPath: Path) {
        val oldPath = Bukkit.getPluginsFolder().toPath().resolve("magnetic.json")
        if (oldPath.exists()) oldPath.moveTo(newPath.createParentDirectories(), overwrite = true)
    }

    private fun registerTranslations() {
        MiniMessageTranslator.loadTranslations()
        GlobalTranslator.translator().addSource(MiniMessageTranslator)
    }

    private fun registerCommand() {
        lifecycleManager.registerEventHandler(
            LifecycleEvents.COMMANDS
        ) { event ->
            event.registrar().register(Command.root.build())
        }
    }
}
