package dev.nyon.magnetic.mixins.fluids;

import dev.nyon.magnetic.holders.FluidPlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {

    @Inject(
        method = "emptyContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BucketItem;playEmptySound(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V",
            shift = At.Shift.AFTER
        )
    )
    private void injectPlayerToFluid(
        LivingEntity entity,
        Level world,
        BlockPos pos,
        BlockHitResult result,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return;
        if (!(world.getFluidState(pos).getType() instanceof FlowingFluid flowingFluid)) return;
        FluidPlayerHolder playerHolder = (FluidPlayerHolder) flowingFluid;
        playerHolder.setPlayer(serverPlayer);
        playerHolder.setPlacedAt(System.currentTimeMillis());
    }
}
