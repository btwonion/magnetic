package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {

    @WrapOperation(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BeehiveBlock;dropHoneycomb(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private void useMagneticInsteadOfShear(
        Level level,
        BlockPos pos,
        Operation<Void> original,
        ItemStack stack,
        BlockState state,
        Level world,
        BlockPos _pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            original.call(level, pos);
            return;
        }
        ServerPlayer previous = threadLocal.get();
        threadLocal.set(serverPlayer);
        try {
            original.call(level, pos);
        } finally {
            threadLocal.set(previous);
        }
    }

    @WrapWithCondition(
        method = "dropHoneycomb",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BeehiveBlock;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private static boolean redirectHoneycomb(
        Level level,
        BlockPos blockPos,
        ItemStack itemStack
    ) {
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return true;

        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, itemStack, blockPos);
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
        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, ((ItemEntity) entity).getItem(), pos);
    }
}
