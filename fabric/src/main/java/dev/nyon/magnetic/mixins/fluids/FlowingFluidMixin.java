package dev.nyon.magnetic.mixins.fluids;

import dev.nyon.magnetic.holders.FluidPlayerHolder;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public class FlowingFluidMixin implements FluidPlayerHolder {
    @Unique
    private ServerPlayer placer = null;
    @Unique
    private Long placedAt = null;

    @Override
    public @Nullable ServerPlayer getPlayer() {
        return placer;
    }

    @Override
    public void setPlayer(@Nullable ServerPlayer serverPlayer) {
        placer = serverPlayer;
    }

    @Override
    public @Nullable Long getPlacedAt() {
        return placedAt;
    }

    @Override
    public void setPlacedAt(@Nullable Long aLong) {
        placedAt = aLong;
    }

    @Inject(
        method = "spreadTo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            shift = At.Shift.AFTER
        )
    )
    private void setPlayerInSpreadTo(
        LevelAccessor world,
        BlockPos pos,
        BlockState state,
        Direction direction,
        FluidState fluidState,
        CallbackInfo ci
    ) {
        ServerPlayer player = MixinHelper.fluidHoldsValidPlayer((FlowingFluid) (Object) this);
        if (player == null) return;
        if (!(world.getFluidState(pos).getType() instanceof FlowingFluid newFluid)) return;
        FluidPlayerHolder playerHolder = (FluidPlayerHolder) newFluid;
        playerHolder.setPlayer(player);
        playerHolder.setPlacedAt(getPlacedAt());
    }
}
