package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public class MobMixin {

    @WrapWithCondition(
        method = "dropCustomDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public boolean modifyCustomDeathLoot(
        Mob instance,
        ServerLevel serverLevel,
        ItemStack itemStack,
        ServerLevel world,
        DamageSource damageSource,
        boolean playerKill
    ) {
        if (!(damageSource.getEntity() instanceof ServerPlayer serverPlayer)) return true;
        return MixinHelper.entityCustomDeathLootSingle(serverPlayer, itemStack, instance, instance.blockPosition());
    }

    @WrapWithCondition(
        method = "dropPreservedEquipment(Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Predicate;)Ljava/util/Set;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public boolean modifyPreservedEquipment(
        Mob instance,
        ServerLevel serverLevel,
        ItemStack itemStack
    ) {
        return MixinHelper.entityDropEquipmentSingle(instance, itemStack, instance.blockPosition());
    }
}
