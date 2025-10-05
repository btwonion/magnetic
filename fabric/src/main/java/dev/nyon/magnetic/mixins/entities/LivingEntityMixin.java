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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    private @Nullable DamageSource lastDamageSource;
    @Unique
    private LivingEntity instance = (LivingEntity) (Object) this;

    // In case the Entity is an Animal manually set the lastDamageSource.
    // Otherwise, the exp will not be handled by the dropExperience function as the damageSource cannot be found somehow
    @Inject(
        method = "actuallyHurt",
        at = @At("HEAD")
    )
    private void setLastDamageSource(
        ServerLevel world,
        DamageSource source,
        float amount,
        CallbackInfo ci
    ) {
        if (!(instance instanceof Animal)) return;
        lastDamageSource = source;
    }

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
        if (MagneticCheckKt.failsLongRangeCheck(lastDamageSource)) return original;
        if (!(entity instanceof ServerPlayer player)) return original;

        return MixinHelper.modifyExpressionValuePlayerExp(player, original, instance.blockPosition());
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
        return MixinHelper.entityCustomDeathLootSingle(damageSource, itemStack, instance, instance.blockPosition());
    }
}