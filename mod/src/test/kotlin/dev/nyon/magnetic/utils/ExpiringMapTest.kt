package dev.nyon.magnetic.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExpiringMapTest {
    @Test
    fun `entry remains available through its expiry instant`() {
        var now = 100L
        val entries = ExpiringMap<String, String> { now }
        entries.put("block", "player", 5_000)

        now = 5_100L
        assertEquals("player", entries["block"])

        now++
        assertNull(entries["block"])
    }

    @Test
    fun `cleanup removes only expired entries`() {
        var now = 100L
        val entries = ExpiringMap<String, String> { now }
        entries.put("short", "one", 100)
        entries.put("long", "two", 1_000)

        now = 201L
        entries.cleanup()

        assertEquals(1, entries.size())
        assertEquals("two", entries["long"])
    }

    @Test
    fun `negative timeout expires immediately`() {
        val entries = ExpiringMap<String, String> { 100L }

        entries.put("block", "player", -1)

        assertNull(entries["block"])
    }
}
