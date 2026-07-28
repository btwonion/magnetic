package dev.nyon.magnetic.mixins.compat.fallingtree;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import fr.rakambda.fallingtree.common.tree.IBreakAttemptResult;
import fr.rakambda.fallingtree.common.tree.Tree;
import fr.rakambda.fallingtree.common.tree.breaking.FallingAnimationTreeBreakingHandler;
import fr.rakambda.fallingtree.common.tree.breaking.InstantaneousTreeBreakingHandler;
import fr.rakambda.fallingtree.common.tree.breaking.ShiftDownTreeBreakingHandler;
import fr.rakambda.fallingtree.common.wrapper.IPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin({
    InstantaneousTreeBreakingHandler.class,
    ShiftDownTreeBreakingHandler.class,
    FallingAnimationTreeBreakingHandler.class
})
public class TreeBreakingHandlerMixin {

    @WrapMethod(method = "breakTree")
    private IBreakAttemptResult scopeTreeBreakToPlayer(
        boolean axeHasBeenUsed,
        IPlayer player,
        Tree tree,
        Operation<IBreakAttemptResult> original
    ) {
        if (!(player.getRaw() instanceof ServerPlayer serverPlayer)) {
            return original.call(axeHasBeenUsed, player, tree);
        }

        return ThreadLocalScope.call(
            threadLocal,
            serverPlayer,
            () -> original.call(axeHasBeenUsed, player, tree)
        );
    }
}
