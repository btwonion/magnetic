package dev.nyon.magnetic.mixins.breakchained;

import dev.nyon.magnetic.BreakChainedPlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Inject(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"
        )
    )
    private void assignPlayerToBreakChainedBlocks(
        BlockPos pos,
        boolean bl,
        Entity entity,
        int i,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return;

        for (Direction direction : Direction.values()) {
            BlockPos checkBlockPos = pos.relative(direction);
            BlockState checkBlockState = getBlockState(checkBlockPos);
            if (checkBlockState.isAir()) continue;
            Block checkBlock = checkBlockState.getBlock();
            if (checkBlock instanceof BreakChainedPlayerHolder) {
                ((BreakChainedPlayerHolder) checkBlock).setInitialBreaker(serverPlayer);
            }
        }
    }
}
