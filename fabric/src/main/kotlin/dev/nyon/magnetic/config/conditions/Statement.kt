package dev.nyon.magnetic.config.conditions

internal val statements: Set<Statement> = setOf(conditions, operators).flatten().toSet()

sealed interface Statement {
    val identifiers: Set<String>
}