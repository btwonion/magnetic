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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public class FlowingFluidMixin {

    @Inject(
        method = "spreadTo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            shift = At.Shift.AFTER
        )
    )
    private void propagateFluidPlayer(
        LevelAccessor world,
        BlockPos pos,
        BlockState state,
        Direction direction,
        FluidState fluidState,
        CallbackInfo ci
    ) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        long timeout = ConfigKt.getConfig()
            .getBuckets()
            .getAbilityTimeout();
        // Check if the source position (opposite direction) has a tracked player
        BlockPos sourcePos = pos.relative(direction.getOpposite());
        ServerPlayer player = tracker.lookupFluid(sourcePos, timeout);
        if (player != null) {
            tracker.record(pos, player, timeout);
        }
    }
}
