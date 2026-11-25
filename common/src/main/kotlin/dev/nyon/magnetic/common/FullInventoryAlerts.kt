package dev.nyon.magnetic.common

import dev.nyon.magnetic.common.config.Config
import java.util.UUID

enum class AlertType {
    SOUND,
    TEXT,
    TITLE
}

class FullInventoryAlerts(currentTimeMillis: () -> Long = System::currentTimeMillis) {
    private val cooldowns = CooldownTracker<Pair<AlertType, UUID>>(currentTimeMillis)

    fun due(settings: Config.FullInventoryAlert, playerId: UUID): List<AlertType> = listOf(
        AlertType.SOUND to settings.soundAlert,
        AlertType.TEXT to settings.textAlert,
        AlertType.TITLE to settings.titleAlert
    ).filter { (type, alert) ->
        alert.enabled && cooldowns.tryAcquire(type to playerId, alert.cooldownInSeconds * 1_000L)
    }.map { (type) -> type }
}
