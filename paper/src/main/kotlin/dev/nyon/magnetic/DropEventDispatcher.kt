package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import org.bukkit.entity.Player
import java.util.UUID

internal data class DropAuthorization(val playerId: UUID)

internal object DropEventDispatcher {
    fun authorize(player: Player): DropAuthorization? {
        if (!config.conditionStatement.checkAndReport(player)) return null
        return DropAuthorization(player.uniqueId)
    }

    fun call(event: DropEvent) {
        val authorization = authorize(event.player) ?: return
        callAuthorized(event, authorization)
    }

    fun callAuthorized(event: DropEvent, authorization: DropAuthorization) {
        require(event.player.uniqueId == authorization.playerId) {
            "Drop authorization belongs to a different player."
        }
        event.callEvent()
    }
}
