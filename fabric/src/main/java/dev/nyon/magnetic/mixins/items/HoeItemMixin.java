package dev.nyon.magnetic.mixins.items;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(HoeItem.class)
public class HoeItemMixin {

    // Check this every version for correctness in the original function
    @ModifyReturnValue(
        method = "changeIntoStateAndDropItem",
        at = @At("RETURN")
    )
    private static Consumer<UseOnContext> changeLambda(Consumer<UseOnContext> original, BlockState result, ItemLike droppedItem) {
        return useOnContext -> {
            if (useOnContext.getPlayer() instanceof ServerPlayer player) {
                if (MixinHelper.wrapWithConditionPlayerItemSingle(player, new ItemStack(droppedItem), useOnContext.getClickedPos())) {
                    useOnContext.getLevel().setBlock(useOnContext.getClickedPos(), result, 11);
                    useOnContext.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, useOnContext.getClickedPos(), GameEvent.Context.of(useOnContext.getPlayer(), result));
                }
                original.accept(useOnContext);
            }
            original.accept(useOnContext);
        };
    }
}
