package dev.nyon.magnetic.mixins.blocks.breakchained;

import dev.nyon.magnetic.BreakChainedPlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseRailBlock.class)
public class BaseRailBlockMixin {

    @Redirect(
        method = "neighborChanged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BaseRailBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private void addPlayerToFunction(
        BlockState blockState,
        Level level,
        BlockPos blockPos
    ) {
        ServerPlayer initialBreaker = ((BreakChainedPlayerHolder) this).getInitialBreaker();
        Block.dropResources(blockState, level, blockPos, null, initialBreaker, Items.AIR.getDefaultInstance());
    }
}
