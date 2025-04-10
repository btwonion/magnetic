package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CaveVines.class)
public interface CaveVinesMixin {

    @WrapWithCondition(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private static boolean doNotDropIfMagnetic(
        Level world,
        BlockPos pos,
        ItemStack stack,
        @Nullable Entity picker,
        BlockState state,
        Level _world,
        BlockPos _pos
    ) {
        if (!(picker instanceof ServerPlayer player)) return true;
        return MixinHelper.wrapWithConditionPlayerItemSingle(player, stack);
    }
}
