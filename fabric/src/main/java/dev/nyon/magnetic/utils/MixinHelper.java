package dev.nyon.magnetic.utils;

import dev.nyon.magnetic.DropEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;

public class MixinHelper {
    public static final ThreadLocal<ServerPlayer> threadLocal = new ThreadLocal<>();

    public static boolean wrapWithConditionPlayerItemSingle(
        ServerPlayer player,
        ItemStack item
    ) {
        ArrayList<ItemStack> mutableList = new ArrayList<>(List.of(item));
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(mutableList, new MutableInt(0), player);

        return !mutableList.isEmpty();
    }

    public static int modifyExpressionValuePlayerExp(
        ServerPlayer player,
        int exp
    ) {
        MutableInt mutableInt = new MutableInt(exp);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(new ArrayList<>(), mutableInt, player);

        return mutableInt.getValue();
    }

    public static boolean entityDropEquipmentSingle(
        LivingEntity entity,
        ItemStack item
    ) {
        LivingEntity lastAttacker = entity.getLastAttacker();
        if (!(lastAttacker instanceof ServerPlayer player)) return true;
        return wrapWithConditionPlayerItemSingle(player, item);
    }

    public static List<ItemStack> entityDropEquipmentMultiple(
        LivingEntity entity,
        List<ItemStack> items
    ) {
        LivingEntity lastAttacker = entity.getLastAttacker();
        if (!(lastAttacker instanceof ServerPlayer player)) return items;

        ArrayList<ItemStack> mutableList = new ArrayList<>(items);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(mutableList, new MutableInt(0), player);

        return mutableList;
    }

    public static boolean entityCustomDeathLootSingle(
        DamageSource source,
        ItemStack item
    ) {
        Entity lastAttacker = source.getEntity();
        if (!(lastAttacker instanceof ServerPlayer player)) return true;

        return wrapWithConditionPlayerItemSingle(player, item);
    }

    public static List<ItemStack> entityCustomDeathLootMultiple(
        DamageSource source,
        List<ItemStack> items
    ) {
        Entity lastAttacker = source.getEntity();
        if (!(lastAttacker instanceof ServerPlayer player)) return items;

        ArrayList<ItemStack> mutableList = new ArrayList<>(items);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(
                mutableList,
                new MutableInt(0),
                player
            );

        return mutableList;
    }
}
