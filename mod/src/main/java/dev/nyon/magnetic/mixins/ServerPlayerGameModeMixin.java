package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.LeafDecayTracker;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;

    @WrapMethod(method = "destroyBlock")
    private boolean scopeDestroyBlock(
        BlockPos pos,
        Operation<Boolean> original
    ) {
        BlockState state = level.getBlockState(pos);
        boolean trackLeafDecay = ConfigKt.getConfig().getLeafDecay().getEnabled()
            && state.is(BlockTags.LOGS)
            && !MagneticCheckKt.isIgnored(state)
            && ConfigKt.getConfig().getConditionStatement().checkAndReport(player);

        boolean destroyed = ThreadLocalScope.call(
            threadLocal,
            player,
            () -> BlockDropScope.call(state, () -> original.call(pos))
        );
        if (!destroyed) return false;

        ServerLevelHolder holder = (ServerLevelHolder) level;
        PositionTracker tracker = holder.getPositionTracker();
        tracker.recordNeighbors(pos, player, level);

        if (trackLeafDecay) {
            LeafDecayTracker leafDecayTracker = holder.getLeafDecayTracker();
            long timeout = ConfigKt.getConfig().getLeafDecay().getAbilityTimeout();
            List<BlockPos> adjacentLeaves = new ArrayList<>();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (level.getBlockState(neighbor).is(BlockTags.LEAVES)) {
                    adjacentLeaves.add(neighbor);
                }
            }
            if (!adjacentLeaves.isEmpty()) {
                leafDecayTracker.record(adjacentLeaves, player.getUUID(), timeout);
            }
        }
        return true;
    }

    @WrapMethod(method = "useItemOn")
    private InteractionResult scopeUseItemOn(
        ServerPlayer interactingPlayer,
        Level world,
        ItemStack stack,
        InteractionHand hand,
        BlockHitResult hitResult,
        Operation<InteractionResult> original
    ) {
        BlockState state = world.getBlockState(hitResult.getBlockPos());
        return ThreadLocalScope.call(
            threadLocal,
            interactingPlayer,
            () -> BlockDropScope.call(
                state,
                () -> original.call(interactingPlayer, world, stack, hand, hitResult)
            )
        );
    }
}
