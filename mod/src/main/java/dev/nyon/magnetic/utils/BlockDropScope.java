package dev.nyon.magnetic.utils;

import dev.nyon.magnetic.extensions.MagneticCheckKt;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

import static dev.nyon.magnetic.utils.MixinHelper.ignoreBlockDrops;

public final class BlockDropScope {

    private BlockDropScope() {
    }

    public static <T> T call(BlockState state, Supplier<T> action) {
        return ThreadLocalScope.call(
            ignoreBlockDrops,
            MagneticCheckKt.isIgnored(state),
            action
        );
    }

    public static void run(BlockState state, Runnable action) {
        call(state, () -> {
            action.run();
            return null;
        });
    }
}
