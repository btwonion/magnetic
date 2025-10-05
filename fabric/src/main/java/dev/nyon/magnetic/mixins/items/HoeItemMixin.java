package dev.nyon.magnetic.mixins.items;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(HoeItem.class)
public class HoeItemMixin {

    @ModifyArgs(
        method = "method_36986",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResourceFromFace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private static void redirectDrops(Args args, BlockState result, ItemLike droppedItem, UseOnContext context) {
        ItemStack original = args.get(3);
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;
        if (MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, original, context.getClickedPos())) return;
        args.set(3, ItemStack.EMPTY);
    }
}
