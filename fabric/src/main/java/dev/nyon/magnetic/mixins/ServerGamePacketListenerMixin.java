package dev.nyon.magnetic.mixins;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
        method = "handleInteract",
        at = @At("HEAD")
    )
    private void setPlayerOnInteract(
        ServerboundInteractPacket packet,
        CallbackInfo ci
    ) {
        threadLocal.set(player);
    }

    @Inject(
        method = "handleInteract",
        at = @At("RETURN")
    )
    private void clearPlayerOnInteract(
        ServerboundInteractPacket packet,
        CallbackInfo ci
    ) {
        threadLocal.remove();
    }
}
