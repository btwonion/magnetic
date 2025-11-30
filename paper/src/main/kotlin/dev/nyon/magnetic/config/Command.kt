package dev.nyon.magnetic.config

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.nyon.konfig.config.loadConfig
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component

object Command {
    val root: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("magnetic").then(
        Commands.literal("reload")
            .requires { it.sender.isOp }
            .executes { ctx ->
                config = loadConfig<Config>()
                reloadIgnoredEntities()
                ctx.source.sender.sendMessage(Component.text("Successfully reloaded config."))
                return@executes Command.SINGLE_SUCCESS
            }
    )
}