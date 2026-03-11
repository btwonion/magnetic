package dev.nyon.magnetic.mixins.compat.th;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Pseudo
@Mixin(targets = "com.natamus.treeharvester_common_fabric.events.LeafEvents")
public class LeafEventsMixin {

    @WrapOperation(
        method = "onWorldTick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/natamus/collective_common_fabric/functions/BlockFunctions;dropBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private static void handleLeafDropWithPlayer(
        Level world,
        BlockPos pos,
        Operation<Void> original,
        ServerLevel level
    ) {
        PositionTracker tracker = ((ServerLevelHolder) level).getPositionTracker();
        ServerPlayer serverPlayer = tracker.lookup(pos);
        if (serverPlayer == null) {
            original.call(world, pos);
            return;
        }
        tracker.recordNeighbors(pos, serverPlayer, level);
        threadLocal.set(serverPlayer);
        try {
            original.call(world, pos);
        } finally {
            threadLocal.remove();
        }
    }
}
