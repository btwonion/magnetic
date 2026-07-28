package dev.nyon.magnetic.mixins;

import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;

    // destroyBlock: set ThreadLocal + record neighbors for chain propagation
    @Inject(
        method = "destroyBlock",
        at = @At("HEAD")
    )
    private void setPlayerOnDestroyBlock(
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        threadLocal.set(player);
        PositionTracker tracker = ((ServerLevelHolder) level).getPositionTracker();
        tracker.recordNeighbors(pos, player, level);
    }

    @Inject(
        method = "destroyBlock",
        at = @At("RETURN")
    )
    private void clearPlayerOnDestroyBlock(
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        threadLocal.remove();
    }

    // useItemOn: covers berry bushes, beehive, pumpkin, cave vines, candle cake, brushable blocks, hoe tilling
    @Inject(
        method = "useItemOn",
        at = @At("HEAD")
    )
    private void setPlayerOnUseItemOn(
        ServerPlayer player,
        Level world,
        ItemStack stack,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        threadLocal.set(player);
    }

    @Inject(
        method = "useItemOn",
        at = @At("RETURN")
    )
    private void clearPlayerOnUseItemOn(
        ServerPlayer player,
        Level world,
        ItemStack stack,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        threadLocal.remove();
    }
}
