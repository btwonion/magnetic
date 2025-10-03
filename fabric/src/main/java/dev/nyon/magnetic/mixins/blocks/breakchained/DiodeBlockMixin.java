package dev.nyon.magnetic.mixins.blocks.breakchained;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DiodeBlock.class)
public class DiodeBlockMixin {

    @Redirect(
        method = "neighborChanged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/DiodeBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
        )
    )
    private void addPlayerToFunction(
        BlockState blockState,
        LevelAccessor levelAccessor,
        BlockPos blockPos,
        BlockEntity blockEntity,
        BlockState state,
        Level world,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean notify
    ) {
        ServerPlayer initialBreaker = MixinHelper.holdsValidPlayer((Block) (Object) this);
        Block.dropResources(blockState, world, blockPos, null, initialBreaker, Items.AIR.getDefaultInstance());
    }
}
