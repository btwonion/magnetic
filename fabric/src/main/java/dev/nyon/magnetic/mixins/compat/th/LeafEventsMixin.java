package dev.nyon.magnetic.mixins.compat.th;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.BreakChainedPlayerHolder;
import dev.nyon.magnetic.utils.CollectiveHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.natamus.treeharvester_common_fabric.events.LeafEvents")
public class LeafEventsMixin {

    @WrapWithCondition(
        method = "onWorldTick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/natamus/collective_common_fabric/functions/BlockFunctions;dropBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private static boolean dontDropIfMagnetic(
        Level world,
        BlockPos pos,
        ServerLevel level
    ) {
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        ServerPlayer serverPlayer = ((BreakChainedPlayerHolder) block).getInitialBreaker();
        if (serverPlayer == null) return true;

        for (Direction direction : Direction.values()) {
            BlockPos checkBlockPos = pos.relative(direction);
            BlockState checkBlockState = level.getBlockState(checkBlockPos);
            if (checkBlockState.isAir()) continue;
            Block checkBlock = checkBlockState.getBlock();
            ((BreakChainedPlayerHolder) checkBlock).setInitialBreaker(serverPlayer);
        }

        CollectiveHelper.dropBlock(blockState, level, pos, null, serverPlayer);

        return false;
    }
}
