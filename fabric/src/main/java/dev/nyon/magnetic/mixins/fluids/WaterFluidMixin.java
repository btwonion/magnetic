package dev.nyon.magnetic.mixins.fluids;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WaterFluid.class)
public class WaterFluidMixin {
    @Redirect(
        method = "beforeDestroyingBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
        )
    )
    private void redirectToPlayerDropResource(
        BlockState state,
        LevelAccessor level,
        BlockPos pos,
        BlockEntity blockEntity
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ServerPlayer player = MixinHelper.fluidHoldsValidPlayer((WaterFluid) (Object) this);
        Block.dropResources(state, serverLevel, pos, blockEntity, player, Items.AIR.getDefaultInstance());
    }
}
