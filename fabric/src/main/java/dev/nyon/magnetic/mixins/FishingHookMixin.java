package dev.nyon.magnetic.mixins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Shadow
    @Nullable
    public abstract Player getPlayerOwner();

    @Inject(
        method = "retrieve",
        at = @At("HEAD")
    )
    private void setPlayerOnRetrieve(
        ItemStack stack,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (getPlayerOwner() instanceof ServerPlayer player) {
            threadLocal.set(player);
        }
    }

    @Inject(
        method = "retrieve",
        at = @At("RETURN")
    )
    private void clearPlayerOnRetrieve(
        ItemStack stack,
        CallbackInfoReturnable<Integer> cir
    ) {
        threadLocal.remove();
    }
}
