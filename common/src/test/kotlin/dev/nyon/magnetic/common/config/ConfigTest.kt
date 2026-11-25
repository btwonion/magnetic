package dev.nyon.magnetic.common.config

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigTest {
    @Test
    fun `identifiers parse ids and tags`() {
        assertEquals(Identifier("minecraft", "stone"), Identifier.parse("stone"))
        assertEquals(Identifier("minecraft", "logs", isTag = true), Identifier.parse("#minecraft:logs"))
        assertEquals("#c:ores/iron", Identifier.parse("#c:ores/iron").toString())
    }

    @Test
    fun `malformed identifiers are rejected`() {
        listOf("Minecraft:stone", "minecraft:", "a:b:c", "#").forEach { value ->
            assertThrows(IllegalArgumentException::class.java, { Identifier.parse(value) }, value)
        }
    }

    @Test
    fun `config round-trips through JSON`() {
        val config = Config(ignoreBlocks = listOf(Identifier.parse("#minecraft:logs")))

        val decoded = Json.decodeFromJsonElement(Config.serializer(), Json.encodeToJsonElement(Config.serializer(), config))

        assertEquals(config.ignoreBlocks, decoded.ignoreBlocks)
        assertEquals("ENCHANTMENT", decoded.conditionStatement.raw)
    }

    @Test
    fun `version 1 configs keep their conditions`() {
        val migrated = migrate(
            Json.parseToJsonElement("""{"needEnchantment":true,"needSneak":true,"itemsAllowed":false,"expAllowed":true}"""),
            1
        )

        assertEquals("ENCHANTMENT && SNEAK", migrated?.conditionStatement?.raw)
        assertEquals(false, migrated?.itemsAllowed)
    }

    @Test
    fun `unknown versions fall back to defaults`() {
        assertNull(migrate(Json.parseToJsonElement("{}"), 2))
    }

    @Test
    fun `enchantment registration follows the condition statement`() {
        assertTrue(configRequiresEnchantment(null))
        assertTrue(configRequiresEnchantment("not json"))
        assertTrue(configRequiresEnchantment("""{"version":6,"config":{"conditionStatement":{"raw":"ENCHANTMENT || SNEAK"}}}"""))
        assertFalse(configRequiresEnchantment("""{"version":6,"config":{"conditionStatement":{"raw":"SNEAK"}}}"""))
        assertFalse(configRequiresEnchantment("""{"version":1,"config":{"needEnchantment":false}}"""))
        assertTrue(configRequiresEnchantment("""{"version":3,"config":{"enchantmentRequired":true}}"""))
    }
}
