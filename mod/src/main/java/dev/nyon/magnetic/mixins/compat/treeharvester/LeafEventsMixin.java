package dev.nyon.magnetic.mixins.compat.treeharvester;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.nyon.magnetic.utils.MixinHelper.conditionAlreadyChecked;
import static dev.nyon.magnetic.utils.MixinHelper.ignoreBlockDrops;
import static dev.nyon.magnetic.utils.MixinHelper.leafDecayAuthorizedPlayer;
import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

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

        UUID playerUuid = ((ServerLevelHolder) serverLevel).getLeafDecayTracker().take(dropPos);
        if (!ConfigKt.getConfig().getLeafDecay().getEnabled() || playerUuid == null) {
            original.call(dropLevel, dropPos);
            return;
        }

        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerUuid);
        if (player == null || player.level() != serverLevel) {
            original.call(dropLevel, dropPos);
            return;
        }

        BlockState state = serverLevel.getBlockState(dropPos);
        BlockDropScope.run(
            state,
            () -> ThreadLocalScope.run(
                threadLocal,
                player,
                () -> ThreadLocalScope.run(
                    conditionAlreadyChecked,
                    true,
                    () -> original.call(dropLevel, dropPos)
                )
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
            && player.getUUID().equals(leafDecayAuthorizedPlayer.get())
            && !Boolean.TRUE.equals(ignoreBlockDrops.get())
            && !MagneticCheckKt.isIgnored(state)) {
            long timeout = ConfigKt.getConfig().getLeafDecay().getAbilityTimeout();
            ((ServerLevelHolder) serverLevel).getLeafDecayTracker().record(
                leafPos,
                player.getUUID(),
                timeout
            );
        }
        return added;
    }
}
