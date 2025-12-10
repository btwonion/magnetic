package dev.nyon.magnetic.mixins.blocks.breakchained;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private ItemEntity redirectBlockDrops(
        FallingBlockEntity instance,
        ServerLevel serverLevel,
        ItemLike itemLike,
        Operation<ItemEntity> original
    ) {
        ServerPlayer initialBreaker = MixinHelper.holdsValidPlayer(instance.getBlockState()
            .getBlock());
        if (initialBreaker == null) return original.call(instance, serverLevel, itemLike);
        return WrapOperationHelper.wrapOperationPlayerItemSingle(
            initialBreaker,
            itemLike.asItem()
                .getDefaultInstance(),
            instance.blockPosition(),
            () -> original.call(instance, serverLevel, itemLike)
        );
    }
}
