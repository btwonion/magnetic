package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(SnowGolem.class)
public class SnowGolemMixin {

    // Saves the player into a ThreadLocal as we cannot get it via the DamageSource
    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/SnowGolem;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void prepareThreadLocalForShearing(
        SnowGolem instance,
        ServerLevel world,
        SoundSource source,
        ItemStack stack,
        Operation<Void> original,
        Player player,
        InteractionHand hand
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            original.call(instance, world, source, stack);
            return;
        }
        WrapOperationHelper.prepareEntity(serverPlayer, instance, () -> original.call(instance, world, source, stack));
    }

    // Consumer of Lnet/minecraft/world/entity/animal/SnowGolem;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V
    @WrapWithCondition(
        method = "method_61476",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/SnowGolem;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private boolean redirectMushroom(
        SnowGolem instance,
        ServerLevel serverLevel,
        ItemStack itemStack,
        float v
    ) {
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return true;
        return MixinHelper.entityWrapWithConditionPlayerItemSingle(serverPlayer, itemStack, instance, instance.blockPosition());
    }
}
