package dev.nyon.magnetic.config.conditions

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

@Serializable
class ConditionChain(val raw: String) {
    @Transient
    private val parsedExpression = runCatching { ConditionExpression.parse(raw) }

    /**
     * Check for the result of the statement and report the error to the chat if the statement is misconfigured.
     *
     * @param player The player the checks should be run against.
     * @return The result of the statement.
     */
    fun checkAndReport(player: ServerPlayer): Boolean {
        try {
            return validate(player)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            player.sendSystemMessage(
                Component.literal("[magnetic] There seems to be an error with the condition statements. Please contact the server administrator to check the logs for more information.")
                    .withStyle(ChatFormatting.RED)
            )
        }
        return false
    }

    /**
     * Validate a parsed expression from left to right, without operator precedence.
     *
     * @param player The player the checks should be run against.
     * @return The result of the statement.
     * @throws IllegalStateException If the expression contains an unknown token or invalid syntax.
     */
    fun validate(player: ServerPlayer): Boolean {
        return parsedExpression.getOrThrow().evaluate { identifier ->
            conditions.single { identifier in it.identifiers }.check(player)
        }
    }

}
