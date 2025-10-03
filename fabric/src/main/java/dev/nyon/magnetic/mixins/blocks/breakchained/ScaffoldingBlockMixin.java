package dev.nyon.magnetic.mixins.blocks.breakchained;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScaffoldingBlock.class)
public class ScaffoldingBlockMixin {

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
        ServerPlayer initialBreaker = MixinHelper.holdsValidPlayer((Block) (Object) this);
        return instance.destroyBlock(blockPos, b, initialBreaker);
    }
}
