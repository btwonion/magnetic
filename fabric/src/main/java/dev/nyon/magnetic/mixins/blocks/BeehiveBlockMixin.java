package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {

    // Check for the correct amount on every update in BeehiveBlock#dropHoneycomb
    @WrapWithCondition(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BeehiveBlock;dropHoneycomb(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private boolean useMagneticInsteadOfShear(
        Level world,
        BlockPos pos,
        ItemStack stack,
        BlockState state,
        Level _world,
        BlockPos _pos,
        Player entity,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (!(entity instanceof ServerPlayer player)) return true;
        return MixinHelper.wrapWithConditionPlayerItemSingle(player, new ItemStack(Items.HONEYCOMB, 3));
    }

    @WrapWithCondition(
        method = "playerWillDestroy",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean useMagneticInsteadOfDrop(
        Level instance,
        Entity entity,
        Level world,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, ((ItemEntity) entity).getItem());
    }
}
