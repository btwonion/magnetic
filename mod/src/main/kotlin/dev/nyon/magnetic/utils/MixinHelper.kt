package dev.nyon.magnetic.utils

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.holders.ServerLevelHolder
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import org.apache.commons.lang3.mutable.MutableInt
import java.util.UUID

object MixinHelper {
    @JvmField
    val threadLocal: ThreadLocal<ServerPlayer> = ThreadLocal<ServerPlayer>()

    @JvmField
    val animationSkip: ThreadLocal<Boolean> = ThreadLocal()

    /**
     * Set while [DropEvent] hands a drop to a player through a transient item entity; accumulates the number of
     * items vanilla inserted into the player's inventory.
     */
    @JvmField
    val directPickup: ThreadLocal<MutableInt> = ThreadLocal()

    @JvmField
    val ignoreBlockDrops: ThreadLocal<Boolean> = ThreadLocal()

    @JvmField
    val ignoreEntityDrops: ThreadLocal<Boolean> = ThreadLocal()

    @JvmField
    val conditionAlreadyChecked: ThreadLocal<Boolean> = ThreadLocal()

    @JvmField
    val leafDecayAuthorizedPlayer: ThreadLocal<UUID> = ThreadLocal()

    @JvmStatic
    fun collectExperience(player: ServerPlayer, exp: Int, pos: BlockPos): Int {
        val mutableInt = MutableInt(exp)
        DropEvent(ArrayList(), mutableInt, player, pos)
        return mutableInt.toInt()
    }

    @JvmStatic
    fun recordLeafDecay(level: ServerLevel, pos: BlockPos) {
        val player = threadLocal.get() ?: return
        if (player.uuid != leafDecayAuthorizedPlayer.get() || ignoreBlockDrops.get() == true) return
        (level as ServerLevelHolder).leafDecayTracker.record(pos, player.uuid, config.leafDecay.abilityTimeout)
    }

    @JvmStatic
    fun playerIn(level: ServerLevel, playerId: UUID?): ServerPlayer? {
        if (playerId == null) return null
        return level.server.playerList.getPlayer(playerId)?.takeIf { it.level() === level }
    }
}
