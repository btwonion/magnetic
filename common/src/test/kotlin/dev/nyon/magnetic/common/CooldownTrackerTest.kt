package dev.nyon.magnetic.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

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
    fun `negative cooldown never blocks acquisition`() {
        val tracker = CooldownTracker<String> { 1_000L }

        assertTrue(tracker.tryAcquire("sound", -1))
        assertTrue(tracker.tryAcquire("sound", -1))
    }

    @Test
    fun `alerts follow the current settings`() {
        var now = 0L
        val alerts = FullInventoryAlerts { now }
        val settings = dev.nyon.magnetic.common.config.Config.FullInventoryAlert()
        val player = UUID.randomUUID()

        assertEquals(listOf(AlertType.SOUND, AlertType.TEXT), alerts.due(settings, player))

        now = 6_000L
        settings.textAlert.enabled = false
        settings.titleAlert.enabled = true
        assertEquals(listOf(AlertType.SOUND, AlertType.TITLE), alerts.due(settings, player))
    }
}
