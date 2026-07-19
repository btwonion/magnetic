@file:Suppress("unused")

package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.ConfigCommand
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.migrate
import java.nio.file.Path

/*? if fabric {*/
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.Commands

fun init() {
    initialize(FabricLoader.getInstance().configDir.resolve("magnetic.json"))
    /*? if <1.21.11 {*/
    /*if (config.conditionStatement.raw.contains("PERMISSION") && runCatching { Class.forName("me.lucko.fabric.api.permissions.v0.Permissions") }.isFailure)
        error("[magnetic] Your condition chain includes a PERMISSION condition, but fabric-permissions-api is not present. Please install it or remove the PERMISSION condition.")
    *//*?}*/
    CommandRegistrationCallback.EVENT.register { dispatcher, _, environment ->
        if (environment != Commands.CommandSelection.DEDICATED) return@register
        ConfigCommand.registerCommand(dispatcher)
    }

    /*? if >=1.21.11 {*/ ServerTickEvents.END_LEVEL_TICK /*?} else {*/ /*ServerTickEvents.END_WORLD_TICK *//*?}*/.register { Animation.tick() }
}

/*?} else if neoforge {*/
/*import dev.nyon.magnetic.config.screen.generateConfigScreen
import dev.nyon.magnetic.extensions.MAGNETIC_PERMISSION
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent
import net.minecraft.commands.Commands
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

@Mod("magnetic")
object MagneticEntrypoint {
    private val gameDir = /*? if >=1.21.9 {*/ FMLLoader.getCurrent().gameDir /*?} else {*/ /*FMLLoader.getGamePath() *//*?}*/
    private val dist = /*? if >=1.21.9 {*/ FMLLoader.getCurrent().dist /*?} else {*/ /*FMLLoader.getDist() *//*?}*/

    init {
        initialize(gameDir.resolve("config/magnetic.json"))
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { event ->
            if (event.commandSelection != Commands.CommandSelection.DEDICATED) return@addListener
            ConfigCommand.registerCommand(event.dispatcher)
        }

        NeoForge.EVENT_BUS.addListener<PermissionGatherEvent.Nodes> { event ->
            event.addNodes(MAGNETIC_PERMISSION)
        }

        if (dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
                IConfigScreenFactory { _, parent -> generateConfigScreen(parent) }
            }
        }

        NeoForge.EVENT_BUS.addListener<ServerTickEvent.Post> {
            Animation.tick()
        }
    }
}
*//*?}*/

private fun initialize(configPath: Path) {
    config(configPath, 5, Config()) { _, element, version ->
        migrate(element, version)
    }
}
