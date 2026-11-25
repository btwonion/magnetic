package dev.nyon.magnetic.config.conditions

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

@Serializable
class ConditionChain(val raw: String) {
    @Transient
    private val parsedStatements = extractStatements(raw)

    fun checkAndReport(player: Player): Boolean {
        try {
            return validate(player)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            player.sendMessage(
                Component.text("[magnetic] There seems to be an error with the condition statements. Please contact the server administrator to check the logs for more information.")
                    .color(NamedTextColor.RED)
            )
        }
        return false
    }

    /** Evaluates from left to right, without operator precedence. */
    fun validate(player: Player): Boolean {
        var firstCondition: Condition? = null
        var operatorToApply: Operator? = null
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

    fun extractStatements(rawStatement: String): List<Statement> {
        val split = rawStatement.split(' ')
        return split.mapNotNull { s ->
            statements.find { it.identifiers.contains(s) }
        }
    }
}