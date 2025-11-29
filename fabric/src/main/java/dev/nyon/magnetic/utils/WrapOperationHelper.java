package dev.nyon.magnetic.utils;

import dev.nyon.magnetic.extensions.MagneticCheckKt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

public class WrapOperationHelper {

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
