package dev.nyon.magnetic.mixins.compat.veinminer;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.miraculixx.veinminer.VeinMinerEvent;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(VeinMinerEvent.class)
public class VeinminerEventMixin {

    @WrapWithCondition(
        method = "improvedDropResources",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private boolean modifyDroppedResources(
        Level level,
        BlockPos pos,
        ItemStack stack,
        BlockState blockState,
        Level _level,
        BlockPos _pos,
        BlockEntity blockEntity,
        Entity breaker,
        ItemStack tool,
        BlockPos initialSource
    ) {
        if (!(breaker instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel)) return true;
        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, stack, pos);
    }

    @WrapOperation(
        method = "improvedDropResources",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"
        )
    )
    private void injectPlayerForSubsequentCalls(
        BlockState instance,
        ServerLevel serverLevel,
        BlockPos blockPos,
        ItemStack itemStack,
        boolean b,
        Operation<Void> original,
        BlockState blockState,
        Level _level,
        BlockPos _pos,
        BlockEntity blockEntity,
        Entity breaker,
        ItemStack tool,
        BlockPos initialSource
    ) {
        if (!(breaker instanceof ServerPlayer serverPlayer)) {
            original.call(instance, serverLevel, blockPos, itemStack, b);
            return;
        }

        ServerPlayer previous = threadLocal.get();
        threadLocal.set(serverPlayer);
        try {
            original.call(instance, serverLevel, blockPos, itemStack, b);
        } finally {
            threadLocal.set(previous);
        }
    }
}
