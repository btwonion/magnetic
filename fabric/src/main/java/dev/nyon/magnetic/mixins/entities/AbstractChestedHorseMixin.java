package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractChestedHorse.class)
public class AbstractChestedHorseMixin {

    @WrapOperation(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractChestedHorse;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public ItemEntity modifyEquipmentDrop(
        AbstractChestedHorse instance,
        ServerLevel serverLevel,
        ItemLike itemLike,
        Operation<ItemEntity> original
    ) {
        LivingEntity lastAttacker = instance.getLastAttacker();
        if (!(lastAttacker instanceof ServerPlayer player)) return original.call(instance, serverLevel, itemLike);
        return WrapOperationHelper.entityWrapOperationPlayerItemSingle(player, new ItemStack(itemLike), instance, instance.blockPosition(), () -> original.call(instance, serverLevel, itemLike));
    }
}