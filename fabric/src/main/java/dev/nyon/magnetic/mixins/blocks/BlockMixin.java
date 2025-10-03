package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.BreakChainedPlayerHolder;
import dev.nyon.magnetic.DropEvent;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(Block.class)
public abstract class BlockMixin implements BreakChainedPlayerHolder {

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

    @Redirect(
        method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z"
        )
    )
    private static boolean useDestroyBlockWithPlayerOnUpdate(
        LevelAccessor instance,
        BlockPos blockPos,
        boolean b,
        Entity entity,
        int maxUpdateDepth
    ) {
        Block toDestroy = instance.getBlockState(blockPos).getBlock();
        ServerPlayer initialBreaker = MixinHelper.holdsValidPlayer(toDestroy);
        return instance.destroyBlock(blockPos, b, initialBreaker, maxUpdateDepth);
    }

    @Unique
    @Nullable ServerPlayer initialBreaker = null;

    @Override
    public @Nullable ServerPlayer getInitialBreaker() {
        return initialBreaker;
    }

    @Override
    public void setInitialBreaker(@Nullable ServerPlayer player) {
        initialBreaker = player;
    }

    @Unique
    @Nullable Long rootBroken = null;

    @Override
    public @Nullable Long getRootBroken() { return rootBroken; }

    @Override
    public void setRootBroken(@Nullable Long rootBroken) { this.rootBroken = rootBroken; }
}
