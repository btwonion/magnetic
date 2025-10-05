package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.ShearableMixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(Sheep.class)
public abstract class SheepMixin {

    @Unique
    private Sheep instance = (Sheep) (Object) this;

    // Saves the player into a ThreadLocal as we cannot get it via the DamageSource
    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void prepareThreadLocalForShearing(
        Sheep instance,
        ServerLevel world,
        SoundSource source,
        ItemStack stack,
        Operation<Void> original,
        Player player,
        InteractionHand hand
    ) {
        ShearableMixinHelper.prepare(player, original, instance, world, source, stack);
    }

    // Consumer of Lnet/minecraft/world/entity/animal/sheep/Sheep;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V
    @ModifyExpressionValue(
        method = "method_61475",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private ItemEntity redirectWool(
        ItemEntity original
    ) {
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return original;
        if (MixinHelper.entityWrapWithConditionPlayerItemSingle(serverPlayer, original.getItem(), instance, instance.blockPosition())) return original;
        return null;
    }
}