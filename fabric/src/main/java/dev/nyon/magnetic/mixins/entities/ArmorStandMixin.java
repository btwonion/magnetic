package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmorStand.class)
public class ArmorStandMixin {

    @Unique
    private ArmorStand instance = (ArmorStand) (Object) this;

    @WrapWithCondition(
        method = "brokenByPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private boolean useMagneticInsteadOfDrop(
        Level world,
        BlockPos pos,
        ItemStack stack,
        ServerLevel serverLevel,
        DamageSource damageSource
    ) {
        return MixinHelper.entityCustomDeathLootSingle(damageSource, stack, instance);
    }

    @WrapWithCondition(
        method = "brokenByAnything",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private boolean useMagneticInsteadOfDropOnAnything(
        Level world,
        BlockPos pos,
        ItemStack stack,
        ServerLevel serverLevel,
        DamageSource damageSource
    ) {
        return MixinHelper.entityCustomDeathLootSingle(damageSource, stack, instance);
    }
}
