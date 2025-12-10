package dev.nyon.magnetic.utils;

import dev.nyon.magnetic.DropEvent;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

public class WrapOperationHelper {

    public static <T> T wrapOperationPlayerItemSingle(
        ServerPlayer player,
        ItemStack item,
        BlockPos pos,
        Supplier<T> callback
    ) {
        ArrayList<ItemStack> mutableList = new ArrayList<>(List.of(item));
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(mutableList, new MutableInt(0), player, pos);

        if (mutableList.isEmpty()) return null;
        return callback.get();
    }

    public static <T> T entityWrapOperationPlayerItemSingle(
        ServerPlayer player,
        ItemStack item,
        Entity instance,
        BlockPos pos,
        Supplier<T> callback
    ) {
        if (MagneticCheckKt.isIgnored(instance.getType())) return callback.get();
        if (MagneticCheckKt.failsLongRangeCheck(instance, player)) return callback.get();
        return wrapOperationPlayerItemSingle(player, item, pos, callback);
    }

    public static void prepareEntity(ServerPlayer player, Entity instance, Runnable callback) {
        if (MagneticCheckKt.isIgnored(instance.getType())) {
            callback.run();
            return;
        }
        prepareGeneral(player, () -> {
            callback.run();
            return null;
        });
    }

    public static <T> T prepareGeneral(ServerPlayer player, Supplier<T> callback) {
        ServerPlayer previous = threadLocal.get();
        threadLocal.set(player);
        try {
            return callback.get();
        } finally {
            threadLocal.set(previous);
        }
    }
}
