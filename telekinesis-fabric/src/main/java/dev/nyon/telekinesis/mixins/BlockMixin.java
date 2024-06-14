package dev.nyon.telekinesis.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.telekinesis.DropEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

import static dev.nyon.telekinesis.utils.MixinHelper.threadLocal;

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
        @Nullable
        BlockEntity blockEntity,
        @Nullable
        Entity entity,
        ItemStack tool
    ) {
        if (!(entity instanceof ServerPlayer player)) return original;

        ArrayList<ItemStack> mutableList = new ArrayList<>(original);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(mutableList, new MutableInt(0), player, tool);

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
        @Nullable
        BlockEntity blockEntity,
        @Nullable
        Entity entity,
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
            target = /*? if >=1.21 {*/ "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processBlockExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I" /*?} else {*/ /*"Lnet/minecraft/util/valueproviders/IntProvider;sample(Lnet/minecraft/util/RandomSource;)I" *//*?}*/
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

        MutableInt mutableInt = new MutableInt(original);
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(new ArrayList<>(), mutableInt, player, heldItem);

        return mutableInt.getValue();
    }
}
