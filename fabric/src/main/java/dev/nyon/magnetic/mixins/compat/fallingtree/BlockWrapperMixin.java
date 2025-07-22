package dev.nyon.magnetic.mixins.compat.fallingtree;

import dev.nyon.magnetic.BreakChainedPlayerHolder;
import fr.rakambda.fallingtree.common.wrapper.*;
import fr.rakambda.fallingtree.fabric.common.wrapper.BlockWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockWrapper.class)
public class BlockWrapperMixin {

    @Inject(
        method = "playerDestroy",
        at = @At("HEAD")
    )
    private void addBreakChainedHolderToBlocks(
        ILevel level,
        IPlayer player,
        IBlockPos blockPos,
        IBlockState blockState,
        IBlockEntity blockEntity,
        IItemStack itemStack,
        boolean dropResources,
        CallbackInfo ci
    ) {
        if (!(player.getRaw() instanceof ServerPlayer serverPlayer)) return;

        for (Direction direction : Direction.values()) {
            BlockPos checkBlockPos = ((BlockPos) blockPos.getRaw()).relative(direction);
            BlockState checkBlockState = ((Level) level.getRaw()).getBlockState(checkBlockPos);
            if (checkBlockState.isAir()) continue;
            Block checkBlock = checkBlockState.getBlock();
            ((BreakChainedPlayerHolder) checkBlock).setInitialBreaker(serverPlayer);
        }
    }
}
