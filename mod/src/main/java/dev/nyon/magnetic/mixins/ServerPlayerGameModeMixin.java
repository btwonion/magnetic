package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static dev.nyon.magnetic.utils.MixinHelper.ignoreBlockDrops;
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
        if (MagneticCheckKt.isIgnored(state)) {
            return ThreadLocalScope.call(ignoreBlockDrops, true, () -> original.call(pos));
        }

        PositionTracker tracker = ((ServerLevelHolder) level).getPositionTracker();
        tracker.recordNeighbors(pos, player, level);
        return ThreadLocalScope.call(threadLocal, player, () -> original.call(pos));
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
        if (MagneticCheckKt.isIgnored(state)) {
            return ThreadLocalScope.call(
                ignoreBlockDrops,
                true,
                () -> original.call(interactingPlayer, world, stack, hand, hitResult)
            );
        }
        return ThreadLocalScope.call(
            threadLocal,
            interactingPlayer,
            () -> original.call(interactingPlayer, world, stack, hand, hitResult)
        );
    }
}
