package dev.nyon.magnetic.config.conditions

/**
 * A parsed Magnetic condition expression.
 *
 * Expressions intentionally evaluate from left to right to preserve the documented config format.
 * Keeping parsing independent from Minecraft makes syntax and truth tables cheap to test.
 */
internal class ConditionExpression private constructor(
    private val firstCondition: String?,
    private val remainingConditions: List<Pair<LogicalOperator, String>>
) {
    fun evaluate(condition: (String) -> Boolean): Boolean {
        val first = firstCondition ?: return true
        return remainingConditions.fold(condition(first)) { result, (operator, name) ->
            when (operator) {
                LogicalOperator.AND -> result && condition(name)
                LogicalOperator.OR -> result || condition(name)
            }
        }
    }

    companion object {
        private val conditionNames = setOf("ENCHANTMENT", "SNEAK", "PERMISSION")

        fun parse(raw: String): ConditionExpression {
            if (raw.isBlank()) return ConditionExpression(null, emptyList())

            val tokens = raw.trim().split(Regex("\\s+"))
            if (tokens.size % 2 == 0) {
                throw IllegalStateException("A condition expression must end with a condition.")
            }

            val first = parseCondition(tokens.first())
            val remaining = tokens.drop(1).chunked(2).map { (operator, condition) ->
                parseOperator(operator) to parseCondition(condition)
            }
            return ConditionExpression(first, remaining)
        }

        private fun parseCondition(token: String): String {
            if (token !in conditionNames) {
                throw IllegalStateException("Unknown condition '$token'.")
            }
            return token
        }

        private fun parseOperator(token: String): LogicalOperator = when (token) {
            "AND", "&&" -> LogicalOperator.AND
            "OR", "||" -> LogicalOperator.OR
            else -> throw IllegalStateException("Unknown condition operator '$token'.")
        }
    }
}

private enum class LogicalOperator {
    AND,
    OR
}
