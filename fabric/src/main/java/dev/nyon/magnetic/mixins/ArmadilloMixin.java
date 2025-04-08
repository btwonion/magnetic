package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Armadillo.class)
public class ArmadilloMixin {

    @WrapWithCondition(
        method = "brushOffScute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/armadillo/Armadillo;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private boolean modifyScuteDrop(
        Armadillo instance,
        ServerLevel serverLevel,
        ItemStack stack
    ) {
        return MixinHelper.entityDropEquipmentSingle(instance, stack);
    }
}
