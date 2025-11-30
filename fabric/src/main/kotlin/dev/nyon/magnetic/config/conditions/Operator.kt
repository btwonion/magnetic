package dev.nyon.magnetic.config.conditions

import net.minecraft.server.level.ServerPlayer

internal val operators: Set<Operator> = setOf(AndOperator, OrOperator)

sealed interface Operator : Statement {
    override val identifiers: Set<String>
    fun apply(boolean: Boolean, condition: Condition, player: ServerPlayer): Boolean
}

object AndOperator : Operator {
    override val identifiers: Set<String> = setOf("AND", "&&")

    override fun apply(boolean: Boolean, condition: Condition, player: ServerPlayer): Boolean {
        return condition.check(player) && boolean
    }
}

object OrOperator : Operator {
    override val identifiers: Set<String> = setOf("OR", "||")

    override fun apply(boolean: Boolean, condition: Condition, player: ServerPlayer): Boolean {
        return condition.check(player) || boolean
    }
}