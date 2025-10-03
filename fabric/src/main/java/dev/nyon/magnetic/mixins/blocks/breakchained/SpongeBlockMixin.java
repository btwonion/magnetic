package dev.nyon.magnetic.mixins.blocks.breakchained;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpongeBlock.class)
public class SpongeBlockMixin {

    @Redirect(
        method = "method_49829",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/SpongeBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
        )
    )
    private static void addPlayerToFunction(
        BlockState blockState,
        LevelAccessor levelAccessor,
        BlockPos blockPos,
        BlockEntity blockEntity
    ) {
        if (blockEntity == null) return;
        ServerPlayer initialBreaker = MixinHelper.holdsValidPlayer(blockState.getBlock());
        Block.dropResources(blockState, blockEntity.getLevel(), blockPos, null, initialBreaker, Items.AIR.getDefaultInstance());
    }
}
