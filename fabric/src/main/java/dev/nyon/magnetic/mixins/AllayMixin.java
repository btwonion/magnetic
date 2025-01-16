package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Allay.class)
public class AllayMixin {

    @Unique
    final Allay instance = (Allay) (Object) this;

    @WrapWithCondition(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/allay/Allay;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public boolean modifyEquipmentDrop(
        Allay instance,
        ServerLevel serverLevel,
        ItemStack stack
    ) {
        return MixinHelper.entityDropEquipmentSingle(instance, stack);
    }

    @ModifyExpressionValue(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/SimpleContainer;removeAllItems()Ljava/util/List;"
        )
    )
    public List<ItemStack> modifyEquipmentDrops(
        List<ItemStack> original
    ) {
        return MixinHelper.entityDropEquipmentMultiple(instance, original);
    }
}
