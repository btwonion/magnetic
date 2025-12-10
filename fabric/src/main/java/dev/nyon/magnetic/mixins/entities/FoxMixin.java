package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Fox.class)
public class FoxMixin {

    @WrapOperation(
        method = "dropAllDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/fox/Fox;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private ItemEntity modifyMouthDrop(
        Fox instance,
        ServerLevel serverLevel,
        ItemStack itemStack,
        Operation<ItemEntity> original
    ) {
        LivingEntity lastAttacker = instance.getLastAttacker();
        if (!(lastAttacker instanceof ServerPlayer player)) return original.call(instance, serverLevel, itemStack);
        return WrapOperationHelper.entityWrapOperationPlayerItemSingle(
            player,
            itemStack,
            instance,
            instance.blockPosition(),
            () -> original.call(instance, serverLevel, itemStack)
        );
    }
}
