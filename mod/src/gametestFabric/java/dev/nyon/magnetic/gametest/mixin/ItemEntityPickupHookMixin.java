package dev.nyon.magnetic.gametest.mixin;

import dev.nyon.magnetic.gametest.TestPickupHooks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks pickup the way Fabric backpack and pickup-filter mods do. */
@Mixin(ItemEntity.class)
public class ItemEntityPickupHookMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void runTestPickupHook(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        if (TestPickupHooks.handle((ItemEntity) (Object) this, player)) ci.cancel();
    }
}
