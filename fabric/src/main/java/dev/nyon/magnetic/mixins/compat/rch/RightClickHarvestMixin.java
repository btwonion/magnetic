package dev.nyon.magnetic.mixins.compat.rch;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import io.github.jamalam360.rightclickharvest.RightClickHarvest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Consumer;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(RightClickHarvest.class)
public class RightClickHarvestMixin {

    @WrapOperation(
        method = "dropStacks",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"
        )
    )
    private static void injectServerPlayer(
        List<ItemStack> instance,
        Consumer<ItemStack> consumer,
        Operation<Void> original,
        BlockState state,
        ServerLevel world,
        BlockPos pos,
        Entity entity,
        ItemStack toolStack,
        boolean removeReplant
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            original.call(instance, consumer);
            return;
        }

        ServerPlayer previous = threadLocal.get();
        threadLocal.set(serverPlayer);
        try {
            original.call(instance, consumer);
        } finally {
            threadLocal.set(previous);
        }
    }

    @WrapWithCondition(
        method = "lambda$dropStacks$8",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private static boolean redirectDroppedStacks(
        Level world,
        BlockPos pos,
        ItemStack stack
    ) {
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return true;

        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, stack);
    }

    @WrapWithCondition(
        method = "completeHarvest",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"
        )
    )
    private static boolean redirectExpDrops(
        Player instance,
        int experience,
        Level level,
        BlockState state,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        ItemStack stackInHand,
        boolean hoeInUse,
        boolean removeReplant,
        Runnable setBlockAction
    ) {
        if (!(instance instanceof ServerPlayer serverPlayer)) return true;
        return MixinHelper.wrapWithConditionPlayerExp(serverPlayer, experience);
    }
}
