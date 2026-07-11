@file:Suppress("unused")

package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.ConfigCommand
import dev.nyon.magnetic.config.migrate
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/*? if fabric {*/
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.Commands

fun init() {
    initialize(FabricLoader.getInstance().configDir.resolve("magnetic.json"))
    CommandRegistrationCallback.EVENT.register { dispatcher, _, environment ->
        if (environment != Commands.CommandSelection.DEDICATED) return@register
        ConfigCommand.registerCommand(dispatcher)
    }
}

private val animationTickRegistered = AtomicBoolean()

internal fun registerAnimationTick() {
    if (!animationTickRegistered.compareAndSet(false, true)) return
    ServerTickEvents.END_LEVEL_TICK.register { Animation.tick() }
}
/*?} else if neoforge {*/
/*import dev.nyon.magnetic.config.screen.generateConfigScreen
import net.minecraft.commands.Commands
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent

@Mod("magnetic")
object MagneticEntrypoint {
    private val loader = FMLLoader.getCurrent()

    init {
        initialize(loader.gameDir.resolve("config/magnetic.json"))
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { event ->
            if (event.commandSelection != Commands.CommandSelection.DEDICATED) return@addListener
            ConfigCommand.registerCommand(event.dispatcher)
        }
        when (loader.dist) {
            Dist.CLIENT -> {
                ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
                    IConfigScreenFactory { _, parent -> generateConfigScreen(parent) }
                }
            }

            Dist.DEDICATED_SERVER -> Unit
        }
    }
}

private val animationTickRegistered = AtomicBoolean()

internal fun registerAnimationTick() {
    if (!animationTickRegistered.compareAndSet(false, true)) return
    NeoForge.EVENT_BUS.addListener<LevelTickEvent.Post> {
        Animation.tick()
    }
}
*//*?}*/

private fun initialize(configPath: Path) {
    config(configPath, 5, Config()) { _, element, version ->
        migrate(element, version)
    }
}
