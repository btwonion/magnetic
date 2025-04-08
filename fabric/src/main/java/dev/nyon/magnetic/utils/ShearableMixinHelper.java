package dev.nyon.magnetic.utils;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.DropEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

public class ShearableMixinHelper {

    public static void prepare(Player player, Operation<Void> original, Entity instance, ServerLevel world, SoundSource source, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            original.call(instance, world, source, stack);
            return;
        }

        ServerPlayer previous = threadLocal.get();
        threadLocal.set(serverPlayer);
        try {
            original.call(instance, world, source, stack);
        } finally {
            threadLocal.set(previous);
        }
    }

    public static BiConsumer<ServerLevel, ItemStack> changeConsumer(LivingEntity instance, BiConsumer<ServerLevel, ItemStack> original) {
        DamageSource source = instance.getLastDamageSource();
        if (source == null || !(source.getEntity() instanceof ServerPlayer player)) return original;

        return (world, item) -> {
            ArrayList<ItemStack> mutableList = new ArrayList<>(List.of(item));
            DropEvent.INSTANCE.getEvent()
                .invoker()
                .invoke(mutableList, new MutableInt(0), player);

            if (!mutableList.isEmpty()) original.accept(world, item);
        };
    }
}
