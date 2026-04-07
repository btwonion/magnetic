package dev.nyon.magnetic.mixins.fluids;

import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(FlowingFluid.class)
public class FlowingFluidMixin {

    @Unique
    private boolean magnetic$setThreadLocal = false;

    @Inject(
        method = "spreadTo",
        at = @At("HEAD")
    )
    private void setFluidPlayer(
        LevelAccessor world,
        BlockPos pos,
        BlockState state,
        Direction direction,
        FluidState fluidState,
        CallbackInfo ci
    ) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        if (threadLocal.get() != null) return;
        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        long timeout = ConfigKt.getConfig()
            .getBuckets()
            .getAbilityTimeout();
        BlockPos sourcePos = pos.relative(direction.getOpposite());
        ServerPlayer player = tracker.lookupFluid(sourcePos, timeout);
        if (player != null) {
            tracker.record(pos, player, timeout);
            threadLocal.set(player);
            magnetic$setThreadLocal = true;
        }
    }

    @Inject(
        method = "spreadTo",
        at = @At("RETURN")
    )
    private void clearFluidPlayer(
        LevelAccessor world,
        BlockPos pos,
        BlockState state,
        Direction direction,
        FluidState fluidState,
        CallbackInfo ci
    ) {
        if (magnetic$setThreadLocal) {
            threadLocal.remove();
            magnetic$setThreadLocal = false;
        }
    }
}
