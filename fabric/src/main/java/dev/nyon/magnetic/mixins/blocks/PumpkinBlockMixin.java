package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(PumpkinBlock.class)
public class PumpkinBlockMixin {

    @WrapOperation(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/PumpkinBlock;dropFromBlockInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Ljava/util/function/BiConsumer;)Z"
        )
    )
    private static boolean doNotDropIfMagnetic(
        ServerLevel serverLevel,
        ResourceKey resourceKey,
        BlockState blockState,
        BlockEntity blockEntity,
        ItemStack itemStack,
        Entity entity,
        BiConsumer biConsumer,
        Operation<Boolean> original
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            original.call(serverLevel, resourceKey, blockState, blockEntity, itemStack, entity, biConsumer);
            return false;
        }
        ServerPlayer previous = threadLocal.get();
        threadLocal.set(serverPlayer);
        try {
            original.call(serverLevel, resourceKey, blockState, blockEntity, itemStack, entity, biConsumer);
        } finally {
            threadLocal.set(previous);
        }
        return false;
    }

    // Consumer of Lnet/minecraft/world/level/block/PumpkinBlock;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;
    @WrapWithCondition(
        method = "method_72609",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private static boolean redirectPumpkinSeeds(
        Level instance,
        Entity entity
    ) {
        if (!(entity instanceof ItemEntity itemEntity)) return true;
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return true;

        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, itemEntity.getItem(), itemEntity.blockPosition());
    }
}
