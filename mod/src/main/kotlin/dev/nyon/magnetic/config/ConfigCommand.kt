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
            Commands.literal("magnetic").then(
                Commands.literal("reload")
                    .requires(/*? if >=1.21.11 {*/ Commands.hasPermission(Commands.LEVEL_ADMINS) /*?} else {*/ /*{ it.hasPermission(2) } *//*?}*/)
                    .executes { ctx ->
                        config = loadConfig<Config>()
                        reloadIgnoredEntities()
                        reloadIgnoredBlocks()
                        ctx.source.sendSystemMessage(Component.literal("Successfully reloaded config."))
                        return@executes Command.SINGLE_SUCCESS
                    }))
    }
}
