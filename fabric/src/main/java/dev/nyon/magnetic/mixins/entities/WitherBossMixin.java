package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {

    @Unique
    private WitherBoss instance = (WitherBoss) (Object) this;

    @ModifyExpressionValue(
        method = "dropCustomDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    protected ItemEntity redirectEquipmentDrop(
        ItemEntity original, ServerLevel world, DamageSource source, boolean playerKill
    ) {
        ItemStack itemStack = original.getItem();
        if (MixinHelper.entityCustomDeathLootSingle(source, itemStack, instance, instance.blockPosition())) return original;
        return null;
    }
}
