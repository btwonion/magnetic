package dev.nyon.magnetic.mixins.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CandleCakeBlock.class)
public class CandleCakeBlockMixin {

    @Redirect(
        method = "useWithoutItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/CandleCakeBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private void addPlayerToFunction(
        BlockState blockState,
        Level level,
        BlockPos blockPos,
        BlockState state,
        Level world,
        BlockPos pos,
        Player entity,
        BlockHitResult hitResult
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) Block.dropResources(blockState, level, blockPos, null, null, Items.AIR.getDefaultInstance());
        else Block.dropResources(blockState, level, blockPos, null, serverPlayer, Items.AIR.getDefaultInstance());
    }
}
