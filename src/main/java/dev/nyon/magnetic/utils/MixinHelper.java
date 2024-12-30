package dev.nyon.magnetic.utils;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.DropEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MixinHelper {
    public static final ThreadLocal<ServerPlayer> threadLocal = new ThreadLocal<>();

    public static boolean wrapWithConditionPlayerItemSingle(
        ServerPlayer player,
        ItemStack item
    ) {
        ArrayList<ItemStack> mutableList = new ArrayList<>(List.of(item));
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(mutableList, new MutableInt(0), player, player.getMainHandItem());

        return !mutableList.isEmpty();
    }

    public static int modifyExpressionValuePlayerExp(
        ServerPlayer player,
        int exp
    ) {
        MutableInt mutableInt = new MutableInt(exp);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(new ArrayList<>(), mutableInt, player, player.getMainHandItem());

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
            .invoke(mutableList, new MutableInt(0), player, player.getMainHandItem());

        return mutableList;
    }

    public static boolean entityCustomDeathLootSingle(
        DamageSource source,
        ItemStack item
    ) {
        Entity lastAttacker = source.getEntity();
        if (!(lastAttacker instanceof ServerPlayer player)) return true;

        ArrayList<ItemStack> mutableList = new ArrayList<>(List.of(item));
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(mutableList,
                new MutableInt(0),
                player,
                Objects.requireNonNullElseGet(source.getWeaponItem(), player::getMainHandItem)
            );

        return !mutableList.isEmpty();
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
            .invoke(mutableList,
                new MutableInt(0),
                player,
                Objects.requireNonNullElseGet(source.getWeaponItem(), player::getMainHandItem)
            );

        return mutableList;
    }

    public static void prepareVehicleServerPlayer(
        VehicleEntity instance,
        Item item,
        Operation<Void> original,
        DamageSource source
    ) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            original.call(instance, item);
            return;
        }

        ServerPlayer previous = threadLocal.get();
        threadLocal.set(player);
        try {
            original.call(instance, item);
        } finally {
            threadLocal.set(previous);
        }
    }

    public static void prepareShearableServerPlayer(
        Shearable instance,
        SoundSource source,
        Operation<Void> original,
        Player _player
    ) {

    }
}
