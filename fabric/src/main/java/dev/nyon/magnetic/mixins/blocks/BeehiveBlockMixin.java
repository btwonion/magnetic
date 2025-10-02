package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {

    @WrapOperation(
        method = "dropHoneycomb",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BeehiveBlock;dropFromBlockInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Ljava/util/function/BiConsumer;)Z"
        )
    )
    private static boolean useMagneticInsteadOfShear(
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

    @WrapWithCondition(
        method = "method_72545",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BeehiveBlock;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private static boolean redirectHoneycomb(
        Level level,
        BlockPos blockPos,
        ItemStack itemStack
    ) {
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return true;

        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, itemStack);
    }

    @WrapWithCondition(
        method = "playerWillDestroy",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean useMagneticInsteadOfDrop(
        Level instance,
        Entity entity,
        Level world,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, ((ItemEntity) entity).getItem());
    }
}
