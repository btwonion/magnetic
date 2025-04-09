package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractHorse.class)
public class AbstractHorseMixin {

    @WrapWithCondition(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public boolean modifyEquipmentDrop(
        AbstractHorse instance,
        ServerLevel serverLevel,
        ItemStack stack
    ) {
        return MixinHelper.entityDropEquipmentSingle(instance, stack);
    }

    @WrapWithCondition(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public boolean modifyEquipmentDrops(
        AbstractHorse instance,
        ServerLevel serverLevel,
        ItemStack stack
    ) {
        return MixinHelper.entityDropEquipmentSingle(instance, stack);
    }
}
