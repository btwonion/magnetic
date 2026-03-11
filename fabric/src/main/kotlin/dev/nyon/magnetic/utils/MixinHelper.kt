package dev.nyon.magnetic.utils

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.holders.ServerLevelHolder
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.mutable.MutableInt

object MixinHelper {
    @JvmField
    val threadLocal: ThreadLocal<ServerPlayer> = ThreadLocal()

    @JvmField
    val animationSkip: ThreadLocal<Boolean> = ThreadLocal()

    @JvmStatic
    fun modifyExpressionValuePlayerExp(player: ServerPlayer, exp: Int, pos: BlockPos): Int {
        val mutableInt = MutableInt(exp)
        DropEvent.event.invoker().invoke(ArrayList(), mutableInt, player, pos)
        return mutableInt.toInt()
    }

    @JvmStatic
    fun tagSurroundingBlocksWithPlayer(player: ServerPlayer, pos: BlockPos, level: ServerLevel) {
        (level as ServerLevelHolder).positionTracker.recordNeighbors(pos, player, level)
    }

    @JvmStatic
    fun wrapWithConditionPlayerItemSingle(player: ServerPlayer, stack: ItemStack, pos: BlockPos): Boolean {
        if (stack.isEmpty) return true
        val items = ArrayList(listOf(stack.copy()))
        val exp = MutableInt(0)
        DropEvent.event.invoker().invoke(items, exp, player, pos)
        return items.isNotEmpty()
    }

    @JvmStatic
    fun wrapWithConditionPlayerExp(player: ServerPlayer, experience: Int, pos: BlockPos): Boolean {
        return modifyExpressionValuePlayerExp(player, experience, pos) > 0
    }
}
