package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

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
        if (!(entity instanceof ServerPlayer player)) return original;
        if (MagneticCheckKt.failsLongRangeCheck(instance, player)) return original;

        return MixinHelper.modifyExpressionValuePlayerExp(player, original, instance.blockPosition());
    }

    @WrapOperation(
        method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V"
        )
    )
    public void prepareForLootTableInjection(
        LivingEntity instance,
        ServerLevel serverLevel,
        DamageSource damageSource,
        boolean b,
        ResourceKey resourceKey,
        Consumer consumer,
        Operation<Void> original
    ) {
        if (!(damageSource.getEntity() instanceof ServerPlayer serverPlayer)) {
            original.call(instance, serverLevel, damageSource, b, resourceKey, consumer);
            return;
        }
        WrapOperationHelper.prepareEntity(
            serverPlayer,
            instance,
            () -> original.call(instance, serverLevel, damageSource, b, resourceKey, consumer)
        );
    }

    // Consumer of Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;)V
    @WrapOperation(
        method = "method_64449",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public ItemEntity redirectCommonDrops(
        LivingEntity instance,
        ServerLevel serverLevel,
        ItemStack itemStack,
        Operation<ItemEntity> original
    ) {
        ServerPlayer player = threadLocal.get();
        if (player == null) return original.call(instance, serverLevel, itemStack);
        return WrapOperationHelper.entityWrapOperationPlayerItemSingle(
            player,
            itemStack,
            instance,
            instance.blockPosition(),
            () -> original.call(instance, serverLevel, itemStack)
        );
    }
}