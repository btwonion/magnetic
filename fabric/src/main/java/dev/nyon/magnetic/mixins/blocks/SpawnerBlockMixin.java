package dev.nyon.magnetic.mixins.blocks;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(SpawnerBlock.class)
public class SpawnerBlockMixin {

    @ModifyVariable(
        method = "spawnAfterBreak",
        at = @At(
            value = "STORE"
        )
    )
    private int modifyDroppedExp(
        int value,
        BlockState state,
        ServerLevel world,
        BlockPos pos,
        ItemStack stack,
        boolean dropExperience
    ) {
        ServerPlayer player = threadLocal.get();
        if (player == null) return value;
        return MixinHelper.modifyExpressionValuePlayerExp(player, value, pos);
    }
}
