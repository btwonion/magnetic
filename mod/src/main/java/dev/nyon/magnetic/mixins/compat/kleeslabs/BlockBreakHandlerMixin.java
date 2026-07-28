package dev.nyon.magnetic.mixins.compat.kleeslabs;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.ThreadLocalScope;
/*? if <1.21.11 {*/
/*import net.blay09.mods.balm.api.event.BreakBlockEvent;
*//*?} else {*/
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/*?}*/
import net.blay09.mods.kleeslabs.BlockBreakHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(BlockBreakHandler.class)
public class BlockBreakHandlerMixin {

    /*? if <1.21.11 {*/
    /*@WrapMethod(method = "onBreakBlock")
    private static void scopeDropsToPlayer(
        BreakBlockEvent event,
        Operation<Void> original
    ) {
        if (!(((BreakBlockEventAccessor) (Object) event).magnetic$getPlayer()
            instanceof ServerPlayer player)) {
            original.call(event);
            return;
        }

        BlockDropScope.run(
            ((BreakBlockEventAccessor) (Object) event).magnetic$getState(),
            () -> ThreadLocalScope.run(threadLocal, player, () -> original.call(event))
        );
    }
    *//*?} else {*/
    @WrapMethod(method = "onBreakBlock")
    private static boolean scopeDropsToPlayer(
        LevelAccessor level,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity,
        Player player,
        Operation<Boolean> original
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return original.call(level, pos, state, blockEntity, player);
        }

        return BlockDropScope.call(
            state,
            () -> ThreadLocalScope.call(
                threadLocal,
                serverPlayer,
                () -> original.call(level, pos, state, blockEntity, player)
            )
        );
    }
    /*?}*/
}
