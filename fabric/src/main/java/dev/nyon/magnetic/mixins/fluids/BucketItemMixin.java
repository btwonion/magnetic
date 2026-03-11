package dev.nyon.magnetic.mixins.fluids;

import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
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
    private void recordBucketPlacement(
        LivingEntity entity,
        Level world,
        BlockPos pos,
        BlockHitResult result,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        if (!ConfigKt.getConfig()
            .getBuckets()
            .getEnabled()) return;

        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        tracker.record(
            pos,
            serverPlayer,
            ConfigKt.getConfig()
                .getBuckets()
                .getAbilityTimeout()
        );
    }
}
