package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Pig.class)
public class PigMixin {

    @WrapWithCondition(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = /*? if needsWorldNow {*//*"Lnet/minecraft/world/entity/animal/Pig;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"*//*?} else {*/  "Lnet/minecraft/world/entity/animal/Pig;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;" /*?}*/
        )
    )
    public boolean modifyEquipmentDrop(
        Pig instance,
        /*$ serverLevel {*//*$}*/
        ItemLike item
    ) {
        return MixinHelper.entityDropEquipmentSingle(instance, new ItemStack(item));
    }
}
