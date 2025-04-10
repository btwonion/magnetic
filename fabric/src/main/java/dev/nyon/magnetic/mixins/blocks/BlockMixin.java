package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.BreakChainedPlayerHolder;
import dev.nyon.magnetic.DropEvent;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(Block.class)
public abstract class BlockMixin {

    @ModifyExpressionValue(
        method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"
        )
    )
    private static List<ItemStack> modifyDrops(
        List<ItemStack> original,
        BlockState state,
        Level level,
        BlockPos pos,
        @Nullable BlockEntity blockEntity,
        @Nullable Entity entity,
        ItemStack tool
    ) {
        if (!(entity instanceof ServerPlayer player)) return original;

        ArrayList<ItemStack> mutableList = new ArrayList<>(original);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(mutableList, new MutableInt(0), player);

        return mutableList;
    }

    @WrapOperation(
        method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"
        )
    )
    private static void checkForPlayerBreak(
        BlockState instance,
        ServerLevel serverLevel,
        BlockPos blockPos,
        ItemStack itemStack,
        boolean b,
        Operation<Void> original,
        BlockState state,
        Level level,
        BlockPos pos,
        @Nullable BlockEntity blockEntity,
        @Nullable Entity entity,
        ItemStack tool
    ) {
        if (!(entity instanceof ServerPlayer player)) {
            original.call(instance, serverLevel, blockPos, itemStack, b);
            return;
        }

        ServerPlayer previous = threadLocal.get();
        threadLocal.set(player);
        try {
            original.call(instance, serverLevel, blockPos, itemStack, b);
        } finally {
            threadLocal.set(previous);
        }
    }

    @ModifyExpressionValue(
        method = "tryDropExperience",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processBlockExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"
        )
    )
    private int modifyExp(
        int original,
        ServerLevel level,
        BlockPos pos,
        ItemStack heldItem,
        IntProvider amount
    ) {
        ServerPlayer player = threadLocal.get();
        if (player == null) return original;

        return MixinHelper.modifyExpressionValuePlayerExp(player, original);
    }

    @Inject(
        method = "playerDestroy",
        at = @At("HEAD")
    )
    private void assignPlayerToBreakChainedBlocks(
        Level world,
        Player player,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity,
        ItemStack stack,
        CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        for (Direction direction : Direction.values()) {
            BlockPos checkBlockPos = pos.relative(direction);
            BlockState checkBlockState = world.getBlockState(checkBlockPos);
            if (checkBlockState.isAir()) continue;
            Block checkBlock = checkBlockState.getBlock();
            if (checkBlock instanceof BreakChainedPlayerHolder) {
                ((BreakChainedPlayerHolder) checkBlock).setInitialBreaker(serverPlayer);
            }
        }
    }
}
