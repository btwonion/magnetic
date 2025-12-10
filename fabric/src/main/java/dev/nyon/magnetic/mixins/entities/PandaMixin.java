package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Panda.class)
public class PandaMixin {

    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/panda/Panda;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private ItemEntity modifyArmorDrop(
        Panda instance,
        ServerLevel serverLevel,
        ItemStack itemStack,
        Operation<ItemEntity> original,
        Player player,
        InteractionHand hand
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return original.call(instance, serverLevel, itemStack);
        return WrapOperationHelper.entityWrapOperationPlayerItemSingle(
            serverPlayer,
            itemStack,
            instance,
            instance.blockPosition(),
            () -> original.call(instance, serverLevel, itemStack)
        );
    }
}
