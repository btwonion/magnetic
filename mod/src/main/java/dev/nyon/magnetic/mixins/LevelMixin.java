package dev.nyon.magnetic.mixins;

import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(Level.class)
public class LevelMixin {

    @Unique
    private Level instance = (Level) (Object) this;

    @Unique
    private boolean magnetic$destroyBlockSetThreadLocal = false;

    @Inject(
        method = "destroyBlock",
        at = @At("HEAD")
    )
    private void setThreadLocalOnDestroyBlock(
        BlockPos pos,
        boolean dropBlock,
        @Nullable Entity entity,
        int maxUpdateDepth,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(instance instanceof ServerLevel serverLevel)) return;
        if (threadLocal.get() != null) return;
        magnetic$destroyBlockSetThreadLocal = true;
        if (entity instanceof ServerPlayer player) {
            threadLocal.set(player);
        } else {
            PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
            ServerPlayer player = tracker.lookup(pos);
            if (player != null) {
                threadLocal.set(player);
                tracker.recordNeighbors(pos, player, serverLevel);
            }
        }
    }

    @Inject(
        method = "destroyBlock",
        at = @At("RETURN")
    )
    private void clearThreadLocalOnDestroyBlock(
        BlockPos pos,
        boolean dropBlock,
        @Nullable Entity entity,
        int maxUpdateDepth,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (magnetic$destroyBlockSetThreadLocal) {
            threadLocal.remove();
            magnetic$destroyBlockSetThreadLocal = false;
        }
    }
}
