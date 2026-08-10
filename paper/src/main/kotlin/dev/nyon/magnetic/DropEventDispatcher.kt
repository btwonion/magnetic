package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import org.bukkit.entity.Player
import java.util.UUID

internal data class DropAuthorization(val playerId: UUID)

internal object DropEventDispatcher {
    private data class AuthorizedDispatch(
        val event: DropEvent,
        val authorization: DropAuthorization
    )

    private val authorizedDispatch = ThreadLocal<AuthorizedDispatch>()

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

        val previousDispatch = authorizedDispatch.get()
        authorizedDispatch.set(AuthorizedDispatch(event, authorization))
        try {
            event.callEvent()
        } finally {
            if (previousDispatch == null) {
                authorizedDispatch.remove()
            } else {
                authorizedDispatch.set(previousDispatch)
            }
        }
    }

    fun isAuthorized(event: DropEvent): Boolean {
        val dispatch = authorizedDispatch.get() ?: return false
        return dispatch.event === event && dispatch.authorization.playerId == event.player.uniqueId
    }
}
