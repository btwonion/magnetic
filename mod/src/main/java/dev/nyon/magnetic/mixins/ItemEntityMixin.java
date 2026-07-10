package dev.nyon.magnetic.mixins;

import dev.nyon.magnetic.Animation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(
        method = "playerTouch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"
        )
    )
    private void invokeItemEntityTake(
        Player player,
        CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer)) return;
        Animation.INSTANCE.invokePickupItemEntity((ItemEntity) (Object) this);
    }
}
