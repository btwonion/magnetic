package dev.nyon.magnetic.utils

import dev.nyon.magnetic.DropEvent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import org.apache.commons.lang3.mutable.MutableInt

object MixinHelper {
    @JvmField
    val threadLocal: ThreadLocal<ServerPlayer> = ThreadLocal<ServerPlayer>()

    @JvmField
    val animationSkip: ThreadLocal<Boolean> = ThreadLocal()

    @JvmField
    val ignoreBlockDrops: ThreadLocal<Boolean> = ThreadLocal()

    @JvmField
    val conditionAlreadyChecked: ThreadLocal<Boolean> = ThreadLocal()

    @JvmStatic
    fun modifyExpressionValuePlayerExp(player: ServerPlayer, exp: Int, pos: BlockPos): Int {
        val mutableInt = MutableInt(exp)
        DropEvent(ArrayList(), mutableInt, player, pos)
        return mutableInt.toInt()
    }
}
