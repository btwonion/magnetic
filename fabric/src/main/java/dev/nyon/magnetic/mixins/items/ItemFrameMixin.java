package dev.nyon.magnetic.mixins.items;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFrame.class)
public class ItemFrameMixin {

    @WrapOperation(
        method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/ItemFrame;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private ItemEntity modifyItemDrop(
        ItemFrame instance,
        ServerLevel serverLevel,
        ItemStack itemStack,
        Operation<ItemEntity> original,
        ServerLevel world,
        @Nullable Entity entity,
        boolean dropSelf
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return original.call(instance, serverLevel, itemStack);
        return WrapOperationHelper.wrapOperationPlayerItemSingle(
            serverPlayer,
            itemStack,
            instance.blockPosition(),
            () -> original.call(instance, serverLevel, itemStack)
        );
    }
}
