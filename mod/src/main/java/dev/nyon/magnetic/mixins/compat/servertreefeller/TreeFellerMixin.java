package dev.nyon.magnetic.mixins.compat.servertreefeller;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.holders.TreeFellerJobHolder;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayDeque;
import java.util.UUID;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

/**
 * Server Tree Feller queues every log of a felled tree and breaks a few of
 * them at the end of each server tick, without a player. Remember who queued
 * each job and scope the deferred break to that player.
 */
@Pseudo
@Mixin(targets = "com.mcstack.treefeller.TreeFeller")
public class TreeFellerMixin {

    @Unique
    private static @Nullable UUID magnetic$polledJobPlayer;

    // The break listener runs inside ServerPlayerGameMode#destroyBlock, where
    // Magnetic has already scoped the breaking player.
    @WrapOperation(
        method = /*? if fabric {*/ "onBlockBroken" /*?} else {*/ /*"onBlockBreak" *//*?}*/,
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/ArrayDeque;add(Ljava/lang/Object;)Z"
        )
    )
    private boolean rememberJobPlayer(
        ArrayDeque<?> queue,
        Object element,
        Operation<Boolean> original
    ) {
        ServerPlayer player = threadLocal.get();
        if (player != null && element instanceof TreeFellerJobHolder job) {
            job.setMagneticPlayer(player.getUUID());
        }
        return original.call(queue, element);
    }

    @WrapOperation(
        method = /*? if fabric {*/ "onServerTickEnd" /*?} else {*/ /*"onServerTick" *//*?}*/,
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/ArrayDeque;poll()Ljava/lang/Object;"
        )
    )
    private Object rememberPolledJob(
        ArrayDeque<?> queue,
        Operation<Object> original
    ) {
        Object job = original.call(queue);
        magnetic$polledJobPlayer = job instanceof TreeFellerJobHolder holder ? holder.getMagneticPlayer() : null;
        return job;
    }

    @WrapOperation(
        method = /*? if fabric {*/ "onServerTickEnd" /*?} else {*/ /*"onServerTick" *//*?}*/,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        )
    )
    private boolean scopeJobDropsToPlayer(
        Level level,
        BlockPos pos,
        boolean dropBlock,
        Operation<Boolean> original
    ) {
        UUID playerUuid = magnetic$polledJobPlayer;
        magnetic$polledJobPlayer = null;
        ServerPlayer player = level instanceof ServerLevel serverLevel ? MixinHelper.playerIn(serverLevel, playerUuid) : null;
        if (player == null) {
            return original.call(level, pos, dropBlock);
        }

        // LevelMixin applies the per-block ignore scope inside destroyBlock.
        return ThreadLocalScope.call(
            threadLocal,
            player,
            () -> original.call(level, pos, dropBlock)
        );
    }
}
