package dev.nyon.magnetic.mixins.breakchained;

import dev.nyon.magnetic.BreakChainedPlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin  implements BreakChainedPlayerHolder {

    @Unique
    @Nullable ServerPlayer initialBreaker = null;

    @Override
    public @Nullable ServerPlayer getInitialBreaker() {
        return initialBreaker;
    }

    @Override
    public void setInitialBreaker(@Nullable ServerPlayer player) {
        initialBreaker = player;
    }

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
        return instance.destroyBlock(blockPos, b, getInitialBreaker());
    }
}