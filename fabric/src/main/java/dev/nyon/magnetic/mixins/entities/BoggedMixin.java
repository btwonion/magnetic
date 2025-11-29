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
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(Bogged.class)
public class BoggedMixin {

    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/Bogged;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void prepareThreadLocalForShearing(
        Bogged instance,
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

    @WrapWithCondition(
        method = "method_61491",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/Bogged;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private boolean changeOriginalDropConsumer(
        Bogged instance,
        ServerLevel serverLevel,
        ItemStack itemStack,
        float v
    ) {
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return true;
        return MixinHelper.entityWrapWithConditionPlayerItemSingle(serverPlayer, itemStack, instance, instance.blockPosition());
    }
}
