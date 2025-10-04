package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherSkeleton.class)
public class WitherSkeletonMixin {

    @WrapWithCondition(
        method = "dropCustomDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/WitherSkeleton;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    public boolean modifyCustomDeathLoot(
        WitherSkeleton instance,
        ServerLevel serverLevel,
        ItemLike itemLike,
        ServerLevel world,
        DamageSource source,
        boolean playerKill
    ) {
        return MixinHelper.entityCustomDeathLootSingle(source, new ItemStack(itemLike), instance, instance.blockPosition());
    }
}
