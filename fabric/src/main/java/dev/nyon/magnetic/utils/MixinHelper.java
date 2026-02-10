package dev.nyon.magnetic.utils;

import dev.nyon.magnetic.BreakChainedPlayerHolder;
import dev.nyon.magnetic.DropEvent;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MixinHelper {
    public static final ThreadLocal<ServerPlayer> threadLocal = new ThreadLocal<>();

    public static boolean entityWrapWithConditionPlayerItemSingle(
        ServerPlayer player,
        ItemStack item,
        Entity instance,
        BlockPos pos
    ) {
        if (MagneticCheckKt.isIgnored(instance.getType())) return true;
        return wrapWithConditionPlayerItemSingle(player, item, pos);
    }

    public static boolean wrapWithConditionPlayerItemSingle(
        ServerPlayer player,
        ItemStack item,
        BlockPos pos
    ) {
        ArrayList<ItemStack> mutableList = new ArrayList<>(List.of(item));
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(mutableList, new MutableInt(0), player, pos);

        return !mutableList.isEmpty();
    }

    public static boolean wrapWithConditionPlayerExp(
        ServerPlayer player,
        int exp,
        BlockPos pos
    ) {
        return modifyExpressionValuePlayerExp(player, exp, pos) != 0;
    }

    public static int modifyExpressionValuePlayerExp(
        ServerPlayer player,
        int exp,
        BlockPos pos
    ) {
        MutableInt mutableInt = new MutableInt(exp);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(new ArrayList<>(), mutableInt, player, pos);

        return (int) mutableInt.get();
    }

    public static List<ItemStack> entityDropEquipmentMultiple(
        LivingEntity entity,
        List<ItemStack> items,
        BlockPos pos
    ) {
        if (MagneticCheckKt.isIgnored(entity.getType())) return items;
        LivingEntity lastAttacker = entity.getLastAttacker();
        return multiple(lastAttacker, items, entity, pos);
    }

    public static boolean entityCustomDeathLootSingle(
        ServerPlayer player,
        ItemStack item,
        Entity instance,
        BlockPos pos
    ) {
        if (MagneticCheckKt.failsLongRangeCheck(instance, player)) return true;
        return entityWrapWithConditionPlayerItemSingle(player, item, instance, pos);
    }

    public static List<ItemStack> entityCustomDeathLootMultiple(
        DamageSource source,
        List<ItemStack> items,
        Entity instance,
        BlockPos pos
    ) {
        if (MagneticCheckKt.isIgnored(instance.getType())) return items;
        Entity lastAttacker = source.getEntity();
        return multiple(lastAttacker, items, instance, pos);
    }

    private static List<ItemStack> multiple(Entity lastAttacker, List<ItemStack> items, Entity instance, BlockPos pos) {
        if (!(lastAttacker instanceof ServerPlayer player)) return items;
        if (MagneticCheckKt.failsLongRangeCheck(instance, player)) return items;

        ArrayList<ItemStack> mutableList = new ArrayList<>(items);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(
                mutableList,
                new MutableInt(0),
                player,
                pos
            );

        return mutableList;
    }

    public static @Nullable ServerPlayer holdsValidPlayer(Block block) {
        BreakChainedPlayerHolder holder = (BreakChainedPlayerHolder) block;
        Long rootBroken = holder.getRootBroken();
        if (rootBroken == null || System.currentTimeMillis() - rootBroken > 5000) {
            holder.setInitialBreaker(null);
            holder.setRootBroken(null);
            return null;
        }
        return holder.getInitialBreaker();
    }

    public static void tagSurroundingBlocksWithPlayer(ServerPlayer serverPlayer, BlockPos rootPos, ServerLevel level) {
        for (Direction direction : Direction.values()) {
            BlockPos checkBlockPos = rootPos.relative(direction);
            BlockState checkBlockState = level.getBlockState(checkBlockPos);
            if (checkBlockState.isAir()) continue;
            Block checkBlock = checkBlockState.getBlock();
            BreakChainedPlayerHolder holder = (BreakChainedPlayerHolder) checkBlock;
            holder.setInitialBreaker(serverPlayer);
            holder.setRootBroken(System.currentTimeMillis());
        }
    }
}
