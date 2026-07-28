package dev.nyon.magnetic.mixins.compat.rightclickharvest;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
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

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(RightClickHarvest.class)
public class RightClickHarvestMixin {

    @WrapMethod(method = "dropStacks")
    private static void scopeBlockDropsToPlayer(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        Entity entity,
        ItemStack tool,
        boolean removeReplant,
        Operation<Void> original
    ) {
        if (!(entity instanceof ServerPlayer player)) {
            original.call(state, level, pos, entity, tool, removeReplant);
            return;
        }

        if (!MagneticCheckKt.isIgnored(state)) {
            PositionTracker tracker = ((ServerLevelHolder) level).getPositionTracker();
            tracker.recordNeighbors(pos, player, level);
        }
        BlockDropScope.run(
            state,
            () -> ThreadLocalScope.run(
                threadLocal,
                player,
                () -> original.call(state, level, pos, entity, tool, removeReplant)
            )
        );
    }

    @WrapOperation(
        method = "completeHarvest",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"
        )
    )
    private static void handleExperience(
        Player experiencePlayer,
        int experience,
        Operation<Void> original,
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
        int remaining = experience;
        if (experience > 0
            && !MagneticCheckKt.isIgnored(state)
            && experiencePlayer instanceof ServerPlayer serverPlayer) {
            remaining = MixinHelper.modifyExpressionValuePlayerExp(serverPlayer, experience, pos);
        }
        original.call(experiencePlayer, remaining);
    }
}
