package dev.nyon.magnetic.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CooldownTrackerTest {
    @Test
    fun `keys have independent cooldowns`() {
        var now = 1_000L
        val tracker = CooldownTracker<String> { now }

        assertTrue(tracker.tryAcquire("sound", 5_000))
        assertFalse(tracker.tryAcquire("sound", 5_000))
        assertTrue(tracker.tryAcquire("text", 5_000))

        now = 6_001L
        assertTrue(tracker.tryAcquire("sound", 5_000))
    }

    @Test
    fun `clear removes cooldown state`() {
        val tracker = CooldownTracker<String> { 1_000L }
        assertTrue(tracker.tryAcquire("sound", 5_000))

        tracker.clear()

        assertTrue(tracker.tryAcquire("sound", 5_000))
    }

    @Test
    fun `negative cooldown never blocks acquisition`() {
        val tracker = CooldownTracker<String> { 1_000L }

        assertTrue(tracker.tryAcquire("sound", -1))
        assertTrue(tracker.tryAcquire("sound", -1))
    }
}
