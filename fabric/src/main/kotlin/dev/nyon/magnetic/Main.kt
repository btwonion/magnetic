@file:Suppress("unused")

package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.ConfigCommand
import dev.nyon.magnetic.config.migrate
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.Commands

fun init() {
    config(FabricLoader.getInstance().configDir.resolve("magnetic.json"), 4, Config()) { _, element, version ->
        migrate(element, version)
    }
    DropEvent
    CommandRegistrationCallback.EVENT.register { dispatcher, _, environment ->
        if (environment != Commands.CommandSelection.DEDICATED) return@register
        ConfigCommand.registerCommand(dispatcher)
    }
}
