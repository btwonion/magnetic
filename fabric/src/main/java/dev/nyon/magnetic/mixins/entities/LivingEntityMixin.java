package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private LivingEntity instance = (LivingEntity) (Object) this;

    @ModifyExpressionValue(
        method = "dropExperience",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getExperienceReward(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)I"
        )
    )
    public int redirectExp(
        int original,
        ServerLevel level,
        Entity entity
    ) {
        if (MagneticCheckKt.isIgnored(instance.getType())) return original;
        if (MagneticCheckKt.failsLongRangeCheck(instance.getLastDamageSource())) return original;
        if (!(entity instanceof ServerPlayer player)) return original;

        return MixinHelper.modifyExpressionValuePlayerExp(player, original);
    }

    // Consumer of Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;)V
    @WrapWithCondition(
        method = "method_64449",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public boolean redirectCommonDrops(
        LivingEntity instance,
        ServerLevel serverLevel,
        ItemStack itemStack
    ) {
        DamageSource damageSource = instance.getLastDamageSource();
        return MixinHelper.entityCustomDeathLootSingle(damageSource, itemStack, instance);
    }
}