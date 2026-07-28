package dev.nyon.magnetic.mixins.compat.treeharvester;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.compat.treeharvester.TreeHarvesterLeafTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Pseudo
@Mixin(
    targets = /*? if fabric {*/
        "com.natamus.treeharvester_common_fabric.processing.LeafProcessing"
        /*?} else {*/
        /*"com.natamus.treeharvester_common_neoforge.processing.LeafProcessing"
        *//*?}*/
)
public class LeafProcessingMixin {

    @WrapOperation(
        method = "breakTreeLeaves",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CopyOnWriteArrayList;add(Ljava/lang/Object;)Z"
        )
    )
    private static boolean rememberQueuedLeafPlayer(
        CopyOnWriteArrayList<?> leaves,
        Object element,
        Operation<Boolean> original,
        Level level,
        List<BlockPos> logs,
        BlockPos bottomLog,
        BlockPos topLog
    ) {
        boolean added = original.call(leaves, element);
        ServerPlayer player = threadLocal.get();
        if (added
            && level instanceof ServerLevel serverLevel
            && element instanceof BlockPos leafPos
            && player != null) {
            TreeHarvesterLeafTracker.record(serverLevel, leafPos, player);
        }
        return added;
    }
}
