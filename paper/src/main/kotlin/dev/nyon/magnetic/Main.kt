package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.magnetic.compat.AuraSkillsCompat
import dev.nyon.magnetic.compat.McMMOCompat
import dev.nyon.magnetic.compat.VeinminerCompat
import dev.nyon.magnetic.config.*
import dev.nyon.magnetic.listeners.BlockListeners
import dev.nyon.magnetic.listeners.DropEventListener
import dev.nyon.magnetic.listeners.ItemListeners
import dev.nyon.magnetic.listeners.FluidListeners
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
        config(dev.nyon.magnetic.configPath, 5, Config()) { _, element, version ->
            migrate(element, version)
        }
        reloadIgnoredEntities()
        registerTranslations()
        registerCommand()
    }

    override fun onEnable() {
        DropEventListener
        ItemListeners
        BlockListeners
        FluidListeners

        if (Bukkit.getPluginManager().isPluginEnabled("mcMMO")) McMMOCompat.listenForEvents()
        if (Bukkit.getPluginManager().isPluginEnabled("Veinminer")) VeinminerCompat.listenForEvents()
        if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills")) AuraSkillsCompat.listenForEvents()
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
        Command.root
    }
}

val Plugin by lazy { Main.INSTANCE }
