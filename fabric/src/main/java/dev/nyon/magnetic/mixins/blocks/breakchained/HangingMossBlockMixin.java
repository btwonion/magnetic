package dev.nyon.magnetic.mixins.blocks.breakchained;

import dev.nyon.magnetic.BreakChainedPlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HangingMossBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HangingMossBlock.class)
public class HangingMossBlockMixin {

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        )
    )
    private boolean usePlayerInDestroyBlock(
        ServerLevel instance,
        BlockPos blockPos,
        boolean b
    ) {
        return instance.destroyBlock(blockPos, b, ((BreakChainedPlayerHolder) this).getInitialBreaker());
    }
}
