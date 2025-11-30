package dev.nyon.magnetic.config.conditions

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

@Serializable
class ConditionChain(val raw: String) {
    @Transient
    private val parsedStatements = extractStatements(raw)

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
                    .withStyle(ChatFormatting.RED))
        }
        return false
    }

    /**
     * Validate a preprocessed set of statements for a single boolean. Currently just follows a start-to-end procedure,
     * so no weights of operators are respected.
     *
     * @param player The player the checks should be run against.
     * @return The result of the statement.
     * @throws IllegalStateException If the statement has an invalid syntax. To check this, it will ignore empty spaces,
     * illegal statements, but it won't accept two statements of the same type followed by one another.
     */
    fun validate(player: ServerPlayer): Boolean {
        // Will be instantiated as soon as the first condition is processed and will not be changed afterward
        var firstCondition: Condition? = null
        // Will be changed everytime an operator is processed
        var operatorToApply: Operator? = null
        // Always set the previously processed statement
        var previousBoolean = true
        parsedStatements.forEachIndexed { index, statement ->
            if (index == 0) if (statement !is Condition) throw IllegalStateException("A statement must start with a condition.")

            when (statement) {
                is Condition -> {
                    if (firstCondition != null) {
                        if (operatorToApply == null) throw IllegalStateException("There cannot be a condition followed by a condition.")
                        previousBoolean = operatorToApply.apply(previousBoolean, statement, player)
                        operatorToApply = null
                    } else {
                        firstCondition = statement
                        previousBoolean = statement.check(player)
                    }
                }
                is Operator -> {
                    if (operatorToApply != null) throw IllegalStateException("There cannot be an operator followed by an operator.")
                    else operatorToApply = statement
                }
            }
        }
        return previousBoolean
    }

    /**
     * Extract conditions and statements from the raw condition chain.
     *
     * @param rawStatement The raw condition chain as a string.
     * @return A list of the statements.
     */
    fun extractStatements(rawStatement: String): List<Statement> {
        val split = rawStatement.split(' ')
        return split.mapNotNull { s ->
            statements.find { it.identifiers.contains(s) }
        }
    }
}