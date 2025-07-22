package dev.nyon.magnetic.mixins.compat.fallingtree;

import fr.rakambda.fallingtree.common.tree.Tree;
import fr.rakambda.fallingtree.common.tree.breaking.LeafForceBreaker;
import fr.rakambda.fallingtree.common.wrapper.IBlockPos;
import fr.rakambda.fallingtree.common.wrapper.IBlockState;
import fr.rakambda.fallingtree.common.wrapper.ILevel;
import fr.rakambda.fallingtree.common.wrapper.IPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LeafForceBreaker.class)
public class LeafForceBreakerMixin {

    @Redirect(
        method = "lambda$forceBreakDecayLeaves$0",
        at = @At(
            value = "INVOKE",
            target = "Lfr/rakambda/fallingtree/common/wrapper/IBlockState;dropResources(Lfr/rakambda/fallingtree/common/wrapper/ILevel;Lfr/rakambda/fallingtree/common/wrapper/IBlockPos;)V"
        )
    )
    private void useCorrectDropResources(
        IBlockState instance,
        ILevel iLevel,
        IBlockPos iBlockPos,
        ILevel _level,
        IPlayer player,
        Tree tree,
        IBlockPos _blockPos
    ) {
        instance.getBlock()
            .playerDestroy(iLevel, player, iBlockPos, instance, null, player.getMainHandItem(), true);
    }
}
