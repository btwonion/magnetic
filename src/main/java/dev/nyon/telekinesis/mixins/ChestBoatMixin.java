package dev.nyon.telekinesis.mixins;

import dev.nyon.telekinesis.utils.MixinHelper;
import net.minecraft.world.entity.vehicle.ChestBoat;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChestBoat.class)
public abstract class ChestBoatMixin {

    /*? if >1.20.2 {*/
    @WrapOperation(
        method = "destroy(Lnet/minecraft/world/damagesource/DamageSource;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/ChestBoat;destroy(Lnet/minecraft/world/item/Item;)V"
        )
    )
    private void checkForPlayer(
        ChestBoat instance,
        Item item,
        Operation<Void> original,
        DamageSource source
    ) {
        MixinHelper.prepareVehicleServerPlayer(instance, item, original, source);
    }
    /*?}*/

    // TODO: boat inventory does still not work!!! as well as furnaces!!
}
