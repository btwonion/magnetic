package dev.nyon.magnetic.mixins.compat.treeharvester;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Pseudo
@Mixin(
    targets = /*? if fabric {*/
        "com.natamus.treeharvester_common_fabric.events.TreeCutEvents"
        /*?} else {*/
        /*"com.natamus.treeharvester_common_neoforge.events.TreeCutEvents"
        *//*?}*/
)
public class TreeCutEventsMixin {

    @WrapOperation(
        method = "onTreeHarvest",
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
        Operation<Void> original,
        Level level,
        Player player,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity
    ) {
        if (!(dropLevel instanceof ServerLevel serverLevel)
            || !(player instanceof ServerPlayer serverPlayer)) {
            original.call(dropLevel, dropPos);
            return;
        }

        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        tracker.recordNeighbors(dropPos, serverPlayer, serverLevel);
        ThreadLocalScope.run(
            threadLocal,
            serverPlayer,
            () -> original.call(dropLevel, dropPos)
        );
    }

    @WrapOperation(
        method = "onTreeHarvest",
        at = @At(
            value = "INVOKE",
            target = /*? if fabric {*/
                "Lcom/natamus/treeharvester_common_fabric/processing/LeafProcessing;breakTreeLeaves(Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"
                /*?} else {*/
                /*"Lcom/natamus/treeharvester_common_neoforge/processing/LeafProcessing;breakTreeLeaves(Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"
                *//*?}*/
        )
    )
    private static void scopeLeafQueueToPlayer(
        Level leafLevel,
        List<BlockPos> logs,
        BlockPos bottomLog,
        BlockPos topLog,
        Operation<Void> original,
        Level level,
        Player player,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            original.call(leafLevel, logs, bottomLog, topLog);
            return;
        }

        ThreadLocalScope.run(
            threadLocal,
            serverPlayer,
            () -> original.call(leafLevel, logs, bottomLog, topLog)
        );
    }
}
