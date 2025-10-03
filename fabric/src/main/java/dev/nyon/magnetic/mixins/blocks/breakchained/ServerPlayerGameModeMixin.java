package dev.nyon.magnetic.mixins.blocks.breakchained;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
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
        MixinHelper.tagSurroundingBlocksWithPlayer(player, pos, level);
    }
}
