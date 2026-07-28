package dev.nyon.magnetic.mixins.compat.fallingtree;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import fr.rakambda.fallingtree.common.tree.Tree;
import fr.rakambda.fallingtree.common.tree.breaking.LeafForceBreaker;
import fr.rakambda.fallingtree.common.wrapper.ILevel;
import fr.rakambda.fallingtree.common.wrapper.IPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(LeafForceBreaker.class)
public class LeafForceBreakerMixin {

    @WrapMethod(method = "forceBreakDecayLeaves")
    private void scopeDropsToPlayer(
        IPlayer player,
        Tree tree,
        ILevel level,
        Operation<Void> original
    ) {
        if (!(player.getRaw() instanceof ServerPlayer serverPlayer)) {
            original.call(player, tree, level);
            return;
        }

        ThreadLocalScope.run(
            threadLocal,
            serverPlayer,
            () -> original.call(player, tree, level)
        );
    }
}
