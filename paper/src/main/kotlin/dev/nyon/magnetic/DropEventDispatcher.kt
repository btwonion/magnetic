package dev.nyon.magnetic

import dev.nyon.magnetic.config.conditions.checkAndReport
import dev.nyon.magnetic.config.config
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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

    fun collect(drops: MutableList<ItemStack>, player: Player, location: Location, exp: Int = 0): Int {
        val itemStacks = drops.toMutableList()
        val remainingExp = MutableInt(exp)
        call(DropEvent(itemStacks, remainingExp, player, location))
        drops.removeIf { item -> itemStacks.none { it.isSimilar(item) } }
        return remainingExp.toInt()
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
