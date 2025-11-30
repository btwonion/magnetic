package dev.nyon.magnetic.config.conditions

import org.bukkit.entity.Player

internal val operators: Set<Operator> = setOf(AndOperator, OrOperator)

sealed interface Operator : Statement {
    override val identifiers: Set<String>
    fun apply(boolean: Boolean, condition: Condition, player: Player): Boolean
}

object AndOperator : Operator {
    override val identifiers: Set<String> = setOf("AND", "&&")

    override fun apply(boolean: Boolean, condition: Condition, player: Player): Boolean {
        return condition.check(player) && boolean
    }
}

object OrOperator : Operator {
    override val identifiers: Set<String> = setOf("OR", "||")

    override fun apply(boolean: Boolean, condition: Condition, player: Player): Boolean {
        return condition.check(player) || boolean
    }
}