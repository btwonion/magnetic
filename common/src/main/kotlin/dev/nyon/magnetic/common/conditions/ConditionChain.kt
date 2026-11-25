package dev.nyon.magnetic.common.conditions

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class ConditionType {
    ENCHANTMENT,
    SNEAK,
    PERMISSION
}

@Serializable
class ConditionChain(val raw: String) {
    @Transient
    private val parsed = runCatching { ConditionExpression.parse(raw) }

    fun evaluate(check: (ConditionType) -> Boolean): Boolean = parsed.getOrThrow().evaluate(check)

    fun references(type: ConditionType): Boolean =
        parsed.getOrNull()?.conditions?.contains(type) ?: raw.contains(type.name)
}

internal class ConditionExpression private constructor(
    private val firstCondition: ConditionType?,
    private val remainingConditions: List<Pair<LogicalOperator, ConditionType>>
) {
    val conditions: Set<ConditionType> =
        setOfNotNull(firstCondition) + remainingConditions.map { (_, condition) -> condition }

    fun evaluate(check: (ConditionType) -> Boolean): Boolean {
        val first = firstCondition ?: return true
        return remainingConditions.fold(check(first)) { result, (operator, condition) ->
            when (operator) {
                LogicalOperator.AND -> result && check(condition)
                LogicalOperator.OR -> result || check(condition)
            }
        }
    }

    companion object {
        fun parse(raw: String): ConditionExpression {
            if (raw.isBlank()) return ConditionExpression(null, emptyList())

            val tokens = raw.trim().split(Regex("\\s+"))
            check(tokens.size % 2 == 1) { "A condition expression must end with a condition." }

            val remaining = tokens.drop(1).chunked(2).map { (operator, condition) ->
                parseOperator(operator) to parseCondition(condition)
            }
            return ConditionExpression(parseCondition(tokens.first()), remaining)
        }

        private fun parseCondition(token: String): ConditionType =
            ConditionType.entries.firstOrNull { it.name == token } ?: error("Unknown condition '$token'.")

        private fun parseOperator(token: String): LogicalOperator = when (token) {
            "AND", "&&" -> LogicalOperator.AND
            "OR", "||" -> LogicalOperator.OR
            else -> error("Unknown condition operator '$token'.")
        }
    }
}

private enum class LogicalOperator {
    AND,
    OR
}
