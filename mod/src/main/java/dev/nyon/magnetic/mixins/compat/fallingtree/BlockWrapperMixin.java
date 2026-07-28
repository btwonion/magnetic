package dev.nyon.magnetic.mixins.compat.fallingtree;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import fr.rakambda.fallingtree.common.wrapper.IBlockEntity;
import fr.rakambda.fallingtree.common.wrapper.IBlockPos;
import fr.rakambda.fallingtree.common.wrapper.IBlockState;
import fr.rakambda.fallingtree.common.wrapper.IItemStack;
import fr.rakambda.fallingtree.common.wrapper.ILevel;
import fr.rakambda.fallingtree.common.wrapper.IPlayer;
/*? if fabric {*/
import fr.rakambda.fallingtree.fabric.common.wrapper.BlockWrapper;
/*?} else {*/
/*import fr.rakambda.fallingtree.neoforge.common.wrapper.BlockWrapper;
*//*?}*/
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(BlockWrapper.class)
public class BlockWrapperMixin {

    @WrapMethod(method = "playerDestroy")
    private void scopeDropsToPlayer(
        ILevel level,
        IPlayer player,
        IBlockPos pos,
        IBlockState state,
        IBlockEntity blockEntity,
        IItemStack tool,
        boolean includeDrops,
        Operation<Void> original
    ) {
        if (!(player.getRaw() instanceof ServerPlayer serverPlayer)) {
            original.call(level, player, pos, state, blockEntity, tool, includeDrops);
            return;
        }

        ThreadLocalScope.run(
            threadLocal,
            serverPlayer,
            () -> original.call(level, player, pos, state, blockEntity, tool, includeDrops)
        );
    }
}
