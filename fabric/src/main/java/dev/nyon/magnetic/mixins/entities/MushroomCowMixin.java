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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(MushroomCow.class)
public class MushroomCowMixin {

    @Unique
    private MushroomCow instance = (MushroomCow) (Object) this;

    // Saves the player into a ThreadLocal as we cannot get it via the DamageSource
    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/MushroomCow;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void prepareThreadLocalForShearing(
        MushroomCow instance,
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

    // Consumer of Lnet/minecraft/world/entity/animal/MushroomCow;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V
    @WrapWithCondition(
        method = "method_61469",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean redirectMushroom(
        ServerLevel serverLevel,
        Entity entity
    ) {
        if (!(entity instanceof ItemEntity itemEntity)) return true;
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return true;
        return MixinHelper.entityWrapWithConditionPlayerItemSingle(serverPlayer, itemEntity.getItem(), instance, instance.blockPosition());
    }
}
