package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Fox.class)
public class FoxMixin {

    @WrapWithCondition(
        method = "dropAllDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Fox;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private boolean modifyMouthDrop(
        Fox instance,
        ServerLevel serverLevel,
        ItemStack stack
    ) {
        return MixinHelper.entityDropEquipmentSingle(instance, stack, instance.blockPosition());
    }
}
