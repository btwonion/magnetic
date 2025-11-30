package dev.nyon.magnetic.config

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import dev.nyon.konfig.config.loadConfig
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object ConfigCommand {
    fun registerCommand(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("magnetic")
                .then(
                    Commands.literal("reload")
                        .requires { it.hasPermission(4) }
                        .executes { ctx ->
                            config = loadConfig<Config>()
                            reloadIgnoredEntities()
                            ctx.source.sendSystemMessage(Component.literal("Successfully reloaded config."))
                            return@executes Command.SINGLE_SUCCESS
                        }
                )
        )
    }
}