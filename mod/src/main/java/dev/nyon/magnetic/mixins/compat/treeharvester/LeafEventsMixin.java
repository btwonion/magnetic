package dev.nyon.magnetic.mixins.compat.treeharvester;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.compat.treeharvester.TreeHarvesterLeafTracker;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;
import static dev.nyon.magnetic.utils.MixinHelper.ignoreBlockDrops;

@Pseudo
@Mixin(
    targets = /*? if fabric {*/
        "com.natamus.treeharvester_common_fabric.events.LeafEvents"
        /*?} else {*/
        /*"com.natamus.treeharvester_common_neoforge.events.LeafEvents"
        *//*?}*/
)
public class LeafEventsMixin {

    @WrapOperation(
        method = "onWorldTick",
        at = @At(
            value = "INVOKE",
            target = /*? if fabric {*/
                "Lcom/natamus/collective_common_fabric/functions/BlockFunctions;dropBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
                /*?} else {*/
                /*"Lcom/natamus/collective_common_neoforge/functions/BlockFunctions;dropBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
                *//*?}*/
        )
    )
    private static void scopeDropsToPlayer(
        Level dropLevel,
        BlockPos dropPos,
        Operation<Void> original
    ) {
        if (!(dropLevel instanceof ServerLevel serverLevel)) {
            original.call(dropLevel, dropPos);
            return;
        }

        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        ServerPlayer player = TreeHarvesterLeafTracker.take(serverLevel, dropPos);
        if (player == null) {
            player = tracker.lookup(dropPos);
        }
        if (player == null) {
            original.call(dropLevel, dropPos);
            return;
        }
        ServerPlayer scopedPlayer = player;

        BlockState state = serverLevel.getBlockState(dropPos);
        if (!MagneticCheckKt.isIgnored(state)) {
            tracker.recordNeighbors(dropPos, scopedPlayer, serverLevel);
        }
        BlockDropScope.run(
            state,
            () -> ThreadLocalScope.run(
                threadLocal,
                scopedPlayer,
                () -> original.call(dropLevel, dropPos)
            )
        );
    }

    @WrapOperation(
        method = "onNeighbourNotify",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CopyOnWriteArrayList;add(Ljava/lang/Object;)Z"
        )
    )
    private static boolean rememberQueuedTickLeafPlayer(
        CopyOnWriteArrayList<?> leaves,
        Object element,
        Operation<Boolean> original,
        Level level,
        BlockPos pos,
        BlockState state,
        EnumSet<Direction> directions,
        boolean moved
    ) {
        boolean added = original.call(leaves, element);
        ServerPlayer player = threadLocal.get();
        if (added
            && level instanceof ServerLevel serverLevel
            && element instanceof BlockPos leafPos
            && player != null
            && !Boolean.TRUE.equals(ignoreBlockDrops.get())
            && !MagneticCheckKt.isIgnored(state)) {
            TreeHarvesterLeafTracker.record(serverLevel, leafPos, player);
        }
        return added;
    }

    @WrapOperation(
        method = "onWorldTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
        )
    )
    private static void scopeLeafTickToPlayer(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        Operation<Void> original
    ) {
        ServerPlayer player = findPlayer(level, pos, false);
        if (player == null) {
            original.call(state, level, pos, random);
            return;
        }

        BlockDropScope.run(
            state,
            () -> ThreadLocalScope.run(
                threadLocal,
                player,
                () -> original.call(state, level, pos, random)
            )
        );
    }

    @WrapOperation(
        method = "onWorldTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
        )
    )
    private static void scopeLeafRandomTickToPlayer(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        Operation<Void> original
    ) {
        ServerPlayer player = findPlayer(level, pos, true);
        if (player == null) {
            original.call(state, level, pos, random);
            return;
        }

        BlockDropScope.run(
            state,
            () -> ThreadLocalScope.run(
                threadLocal,
                player,
                () -> original.call(state, level, pos, random)
            )
        );
    }

    @Inject(method = "onWorldTick", at = @At("TAIL"))
    private static void cleanupLeafPlayers(ServerLevel level, CallbackInfo ci) {
        TreeHarvesterLeafTracker.cleanup(level);
    }

    private static ServerPlayer findPlayer(
        ServerLevel level,
        BlockPos pos,
        boolean consume
    ) {
        ServerPlayer player = consume
            ? TreeHarvesterLeafTracker.take(level, pos)
            : TreeHarvesterLeafTracker.lookup(level, pos);
        if (player != null) return player;

        PositionTracker tracker = ((ServerLevelHolder) level).getPositionTracker();
        return tracker.lookup(pos);
    }
}
