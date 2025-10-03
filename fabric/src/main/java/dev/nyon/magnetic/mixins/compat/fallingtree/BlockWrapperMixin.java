package dev.nyon.magnetic.mixins.compat.fallingtree;

import dev.nyon.magnetic.utils.MixinHelper;
import fr.rakambda.fallingtree.common.wrapper.*;
import fr.rakambda.fallingtree.fabric.common.wrapper.BlockWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        MixinHelper.tagSurroundingBlocksWithPlayer(serverPlayer, (BlockPos) blockPos.getRaw(), (ServerLevel) level.getRaw());
    }
}
