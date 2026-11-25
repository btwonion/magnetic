package dev.nyon.magnetic.common.conditions

import dev.nyon.magnetic.common.conditions.ConditionType.ENCHANTMENT
import dev.nyon.magnetic.common.conditions.ConditionType.PERMISSION
import dev.nyon.magnetic.common.conditions.ConditionType.SNEAK
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConditionChainTest {
    @Test
    fun `empty statement is always active`() {
        assertTrue(ConditionChain(" ").evaluate { false })
    }

    @Test
    fun `conditions evaluate from left to right`() {
        val values = mapOf(ENCHANTMENT to true, SNEAK to false, PERMISSION to false)

        assertFalse(ConditionChain("ENCHANTMENT OR SNEAK AND PERMISSION").evaluate(values::getValue))
    }

    @Test
    fun `symbolic and word operators are accepted`() {
        val values = mapOf(ENCHANTMENT to true, SNEAK to false, PERMISSION to true)

        assertTrue(ConditionChain("ENCHANTMENT && PERMISSION || SNEAK").evaluate(values::getValue))
    }

    @Test
    fun `malformed statements never evaluate`() {
        listOf("ENCHANTMEN", "ENCHANTMENT XOR SNEAK", "AND ENCHANTMENT", "ENCHANTMENT AND").forEach { raw ->
            assertThrows(IllegalStateException::class.java, { ConditionChain(raw).evaluate { true } }, raw)
        }
    }

    @Test
    fun `references reports the conditions a statement uses`() {
        val chain = ConditionChain("SNEAK || PERMISSION")

        assertFalse(chain.references(ENCHANTMENT))
        assertTrue(chain.references(SNEAK))
        assertTrue(chain.references(PERMISSION))
    }

    @Test
    fun `malformed statements reference every condition they name`() {
        val chain = ConditionChain("ENCHANTMENT XOR SNEAK")

        assertTrue(chain.references(ENCHANTMENT))
        assertFalse(chain.references(PERMISSION))
    }
}
