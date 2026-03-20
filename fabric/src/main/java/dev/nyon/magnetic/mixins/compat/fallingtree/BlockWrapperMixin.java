package dev.nyon.magnetic.mixins.compat.fallingtree;

import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import fr.rakambda.fallingtree.common.wrapper.*;
import fr.rakambda.fallingtree.fabric.common.wrapper.BlockWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(BlockWrapper.class)
public class BlockWrapperMixin {

    @Inject(
        method = "playerDestroy",
        at = @At("HEAD")
    )
    private void setPlayerOnDestroy(
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
        if (!(level.getRaw() instanceof ServerLevel serverLevel)) return;
        threadLocal.set(serverPlayer);
        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        tracker.recordNeighbors((BlockPos) blockPos.getRaw(), serverPlayer, serverLevel);
    }

    @Inject(
        method = "playerDestroy",
        at = @At("RETURN")
    )
    private void clearPlayerOnDestroy(
        ILevel level,
        IPlayer player,
        IBlockPos blockPos,
        IBlockState blockState,
        IBlockEntity blockEntity,
        IItemStack itemStack,
        boolean dropResources,
        CallbackInfo ci
    ) {
        threadLocal.remove();
    }
}
