package dev.nyon.magnetic.compat

import dev.nyon.magnetic.Main
import org.bukkit.Bukkit

/**
 * An optional integration, registered on enable when its requirements are met.
 *
 * [register] is a lambda so the integration's classes, which reference compile-only plugin APIs, are only
 * loaded once the plugin is known to be present.
 */
internal class PluginCompat(
    val name: String,
    /** Plugin that must be enabled, or null when the integration does not depend on a plugin. */
    private val plugin: String? = null,
    /** Class that must be loadable. Guards against unrelated plugins that share [plugin]'s name. */
    private val requiredClass: String? = null,
    private val register: () -> Unit
) {
    fun isAvailable(): Boolean {
        if (plugin != null && !Bukkit.getPluginManager().isPluginEnabled(plugin)) return false
        return requiredClass == null || isClassPresent(requiredClass)
    }

    fun register() = register.invoke()

    private fun isClassPresent(className: String): Boolean {
        return try {
            Class.forName(className, false, PluginCompat::class.java.classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }
}

/**
 * Every optional integration of the Paper plugin.
 *
 * To add one: put the integration in this package, add a `compileOnly` dependency and an optional
 * `dependencies.server` entry in `build.gradle.kts` if it uses a plugin API, and list it here.
 * Listeners are registered in list order, which matters for listeners on the same event and priority.
 */
internal object PluginCompats {
    private val compats = listOf(
        PluginCompat("mcMMO", plugin = "mcMMO") { McMMOCompat.listenForEvents() },
        // Other plugins may share the "Veinminer" name, so the event class is checked instead of the plugin name.
        PluginCompat(
            "Veinminer",
            requiredClass = "de.miraculixx.veinminer.VeinMinerEvent\$VeinminerDropEvent"
        ) { VeinminerCompat.listenForEvents() },
        PluginCompat("AuraSkills", plugin = "AuraSkills") { AuraSkillsCompat.listenForEvents() },
        // Datapack; enables its listeners itself once the server has loaded its datapacks.
        PluginCompat("TreeCapitator") { TreeCapitatorCompat.listenForEvents() },
        PluginCompat("block break drops") { BlockBreakDropCapture.listenForEvents() },
        PluginCompat("auto-smelt datapacks") { SmeltingDatapackCompat.listenForEvents() },
        // Other plugins may also be named "TreeFeller"; only hook into the one exposing this API.
        PluginCompat(
            "Tree Feller",
            plugin = "TreeFeller",
            requiredClass = "com.thizthizzydizzy.treefeller.compat.TreeFellerCompat"
        ) { TreeFellerCompat.register() },
        // Other plugins may also be named "TimberZ"; only hook into ZetaPlugins' plugin.
        PluginCompat(
            "TimberZ",
            plugin = "TimberZ",
            requiredClass = "com.zetaplugins.timberz.TimberZ"
        ) { TimberZCompat.register() }
    )

    fun registerAvailable() {
        compats.forEach { compat ->
            if (!compat.isAvailable()) return@forEach
            fun warn(e: Throwable) = Main.INSTANCE.logger.warning("Failed to enable ${compat.name} compatibility: $e")
            try {
                compat.register()
            } catch (e: Exception) {
                warn(e)
            } catch (e: LinkageError) {
                warn(e)
            }
        }
    }
}
