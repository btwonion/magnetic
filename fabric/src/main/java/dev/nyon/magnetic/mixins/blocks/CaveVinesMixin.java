package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(CaveVines.class)
public interface CaveVinesMixin {

    @WrapOperation(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;dropFromBlockInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Ljava/util/function/BiConsumer;)Z"
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
        if (!(entity instanceof ServerPlayer serverPlayer))
            return original.call(serverLevel, resourceKey, blockState, blockEntity, itemStack, entity, biConsumer);
        return WrapOperationHelper.prepareGeneral(
            serverPlayer,
            () -> original.call(serverLevel, resourceKey, blockState, blockEntity, itemStack, entity, biConsumer)
        );
    }

    // Consumer of Lnet/minecraft/world/level/block/CaveVines;use(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/InteractionResult;
    @WrapWithCondition(
        method = "method_72578",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private static boolean redirectGlowBerries(
        Level level,
        BlockPos blockPos,
        ItemStack itemStack
    ) {
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return true;

        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, itemStack, blockPos);
    }
}
