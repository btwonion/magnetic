package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Pseudo
@Mixin(targets = "net.minecraft.world.entity.vehicle.VehicleEntity")
public class VehicleEntityMixin {
    @WrapWithCondition(
        method = "destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/Item;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private boolean replaceDropItem(
        VehicleEntity instance,
        ServerLevel world,
        ItemStack itemStack
    ) {
        ServerPlayer player = threadLocal.get();
        if (player == null) return true;

        return MixinHelper.wrapWithConditionPlayerItemSingle(player, itemStack);
    }
}