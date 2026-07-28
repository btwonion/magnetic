package dev.nyon.magnetic.mixins.fluids;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(FlowingFluid.class)
public class FlowingFluidMixin {

    @WrapMethod(method = "spreadTo")
    private void scopeFluidDrops(
        LevelAccessor world,
        BlockPos pos,
        BlockState state,
        Direction direction,
        FluidState fluidState,
        Operation<Void> original
    ) {
        Runnable action = () -> original.call(world, pos, state, direction, fluidState);
        if (!(world instanceof ServerLevel serverLevel) || threadLocal.get() != null) {
            BlockDropScope.run(state, action);
            return;
        }

        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        long timeout = ConfigKt.getConfig()
            .getBuckets()
            .getAbilityTimeout();
        BlockPos sourcePos = pos.relative(direction.getOpposite());
        ServerPlayer player = tracker.lookupFluid(sourcePos, timeout);
        if (player == null) {
            BlockDropScope.run(state, action);
            return;
        }

        tracker.record(pos, player, timeout);
        BlockDropScope.run(
            state,
            () -> ThreadLocalScope.run(threadLocal, player, action)
        );
    }
}
