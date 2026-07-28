package dev.nyon.magnetic.mixins.compat.fallingtree;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import fr.rakambda.fallingtree.common.leaf.LeafBreakingHandler;
import fr.rakambda.fallingtree.common.leaf.LeafBreakingSchedule;
import fr.rakambda.fallingtree.common.wrapper.IBlockPos;
import fr.rakambda.fallingtree.common.wrapper.IBlockState;
import fr.rakambda.fallingtree.common.wrapper.IRandomSource;
import fr.rakambda.fallingtree.common.wrapper.IServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;
import static dev.nyon.magnetic.utils.MixinHelper.ignoreBlockDrops;

@Mixin(LeafBreakingHandler.class)
public class LeafBreakingHandlerMixin {

    private static final long SCHEDULE_TIMEOUT_MILLIS = 5_000L;

    @WrapOperation(
        method = "onBlockUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lfr/rakambda/fallingtree/common/leaf/LeafBreakingHandler;addSchedule(Lfr/rakambda/fallingtree/common/leaf/LeafBreakingSchedule;)V"
        )
    )
    private void rememberScheduledLeafPlayer(
        LeafBreakingHandler instance,
        LeafBreakingSchedule schedule,
        Operation<Void> original
    ) {
        original.call(instance, schedule);

        ServerPlayer player = threadLocal.get();
        if (player == null
            || Boolean.TRUE.equals(ignoreBlockDrops.get())
            || !(schedule.getLevel().getRaw() instanceof ServerLevel level)
            || !(schedule.getBlockPos().getRaw() instanceof BlockPos pos)) {
            return;
        }

        PositionTracker tracker = ((ServerLevelHolder) level).getPositionTracker();
        tracker.record(pos, player, SCHEDULE_TIMEOUT_MILLIS);
    }

    @WrapOperation(
        method = "onServerTick",
        at = @At(
            value = "INVOKE",
            target = "Lfr/rakambda/fallingtree/common/wrapper/IBlockState;tick(Lfr/rakambda/fallingtree/common/wrapper/IServerLevel;Lfr/rakambda/fallingtree/common/wrapper/IBlockPos;Lfr/rakambda/fallingtree/common/wrapper/IRandomSource;)V"
        )
    )
    private void scopeScheduledLeafTickToPlayer(
        IBlockState state,
        IServerLevel level,
        IBlockPos pos,
        IRandomSource random,
        Operation<Void> original
    ) {
        runWithScheduledPlayer(
            state,
            level,
            pos,
            () -> original.call(state, level, pos, random)
        );
    }

    @WrapOperation(
        method = "onServerTick",
        at = @At(
            value = "INVOKE",
            target = "Lfr/rakambda/fallingtree/common/wrapper/IBlockState;randomTick(Lfr/rakambda/fallingtree/common/wrapper/IServerLevel;Lfr/rakambda/fallingtree/common/wrapper/IBlockPos;Lfr/rakambda/fallingtree/common/wrapper/IRandomSource;)V"
        )
    )
    private void scopeScheduledLeafRandomTickToPlayer(
        IBlockState state,
        IServerLevel level,
        IBlockPos pos,
        IRandomSource random,
        Operation<Void> original
    ) {
        runWithScheduledPlayer(
            state,
            level,
            pos,
            () -> original.call(state, level, pos, random)
        );
    }

    private static void runWithScheduledPlayer(
        IBlockState wrappedState,
        IServerLevel wrappedLevel,
        IBlockPos wrappedPos,
        Runnable action
    ) {
        if (!(wrappedLevel.getRaw() instanceof ServerLevel level)
            || !(wrappedPos.getRaw() instanceof BlockPos pos)) {
            action.run();
            return;
        }

        PositionTracker tracker = ((ServerLevelHolder) level).getPositionTracker();
        ServerPlayer player = tracker.lookup(pos);
        if (player == null) {
            action.run();
            return;
        }

        if (wrappedState.getRaw() instanceof BlockState state) {
            if (!MagneticCheckKt.isIgnored(state)) {
                tracker.recordNeighbors(pos, player, level);
            }
            BlockDropScope.run(
                state,
                () -> ThreadLocalScope.run(threadLocal, player, action)
            );
        } else {
            tracker.recordNeighbors(pos, player, level);
            ThreadLocalScope.run(threadLocal, player, action);
        }
    }
}
