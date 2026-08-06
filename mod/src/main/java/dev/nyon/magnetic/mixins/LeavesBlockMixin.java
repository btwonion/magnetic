package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.LeafDecayTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

import static dev.nyon.magnetic.utils.MixinHelper.conditionAlreadyChecked;
import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {

    @WrapMethod(method = "tick")
    private void propagateLeafDecayAttribution(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        Operation<Void> original
    ) {
        original.call(state, level, pos, random);
        if (ConfigKt.getConfig().getLeafDecay().getEnabled()) {
            ((ServerLevelHolder) level).getLeafDecayTracker().propagate(pos, level);
        }
    }

    @WrapMethod(method = "randomTick")
    private void scopeNaturalLeafDecay(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        Operation<Void> original
    ) {
        boolean decaying = !state.getValue(LeavesBlock.PERSISTENT)
            && state.getValue(LeavesBlock.DISTANCE) == LeavesBlock.DECAY_DISTANCE;
        if (!decaying) {
            original.call(state, level, pos, random);
            return;
        }

        LeafDecayTracker tracker = ((ServerLevelHolder) level).getLeafDecayTracker();
        UUID playerUuid = tracker.take(pos);
        if (!ConfigKt.getConfig().getLeafDecay().getEnabled() || playerUuid == null) {
            original.call(state, level, pos, random);
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
        if (player == null || player.level() != level) {
            original.call(state, level, pos, random);
            return;
        }

        ThreadLocalScope.run(
            threadLocal,
            player,
            () -> ThreadLocalScope.run(
                conditionAlreadyChecked,
                true,
                () -> BlockDropScope.run(
                    state,
                    () -> original.call(state, level, pos, random)
                )
            )
        );
    }
}
