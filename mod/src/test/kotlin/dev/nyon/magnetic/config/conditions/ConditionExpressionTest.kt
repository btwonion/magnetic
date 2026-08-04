package dev.nyon.magnetic.config.conditions

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConditionExpressionTest {
    @Test
    fun `empty expression is always active`() {
        assertTrue(ConditionExpression.parse(" ").evaluate { false })
    }

    @Test
    fun `conditions evaluate from left to right`() {
        val values = mapOf("ENCHANTMENT" to true, "SNEAK" to false, "PERMISSION" to false)

        val result = ConditionExpression.parse("ENCHANTMENT OR SNEAK AND PERMISSION")
            .evaluate { values.getValue(it) }

        assertFalse(result)
    }

    @Test
    fun `symbolic and word operators are accepted`() {
        val values = mapOf("ENCHANTMENT" to true, "SNEAK" to false, "PERMISSION" to true)

        assertTrue(
            ConditionExpression.parse("ENCHANTMENT && PERMISSION || SNEAK")
                .evaluate { values.getValue(it) }
        )
    }

    @Test
    fun `unknown condition is rejected`() {
        assertThrows(IllegalStateException::class.java) {
            ConditionExpression.parse("ENCHANTMEN")
        }
    }

    @Test
    fun `unknown operator is rejected`() {
        assertThrows(IllegalStateException::class.java) {
            ConditionExpression.parse("ENCHANTMENT XOR SNEAK")
        }
    }

    @Test
    fun `leading and trailing operators are rejected`() {
        assertThrows(IllegalStateException::class.java) {
            ConditionExpression.parse("AND ENCHANTMENT")
        }
        assertThrows(IllegalStateException::class.java) {
            ConditionExpression.parse("ENCHANTMENT AND")
        }
    }
}
