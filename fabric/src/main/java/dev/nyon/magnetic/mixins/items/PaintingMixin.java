package dev.nyon.magnetic.mixins.items;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Painting.class)
public class PaintingMixin {

    @WrapOperation(
        method = "dropItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/painting/Painting;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private ItemEntity modifyPaintDrop(
        Painting instance,
        ServerLevel serverLevel,
        ItemLike itemLike,
        Operation<ItemEntity> original,
        ServerLevel world,
        @Nullable Entity entity
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return original.call(instance, serverLevel, itemLike);
        return WrapOperationHelper.wrapOperationPlayerItemSingle(
            serverPlayer,
            new ItemStack(itemLike),
            instance.blockPosition(),
            () -> original.call(instance, serverLevel, itemLike)
        );
    }
}
