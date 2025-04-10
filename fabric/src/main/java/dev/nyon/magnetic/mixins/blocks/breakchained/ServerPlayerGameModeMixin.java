package dev.nyon.magnetic.mixins.blocks.breakchained;

import dev.nyon.magnetic.BreakChainedPlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;

    @Inject(
        method = "destroyBlock",
        at = @At("HEAD")
    )
    private void assignPlayerToBreakChainedBlocks(
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        for (Direction direction : Direction.values()) {
            BlockPos checkBlockPos = pos.relative(direction);
            BlockState checkBlockState = level.getBlockState(checkBlockPos);
            if (checkBlockState.isAir()) continue;
            Block checkBlock = checkBlockState.getBlock();
            ((BreakChainedPlayerHolder) checkBlock).setInitialBreaker(player);
        }
    }
}
