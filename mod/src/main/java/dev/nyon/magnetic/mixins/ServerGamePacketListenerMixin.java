package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerMixin {

    @Shadow
    public ServerPlayer player;

    @WrapMethod(method = "handleInteract")
    private void scopeInteractToPlayer(
        ServerboundInteractPacket packet,
        Operation<Void> original
    ) {
        ThreadLocalScope.run(threadLocal, player, () -> original.call(packet));
    }
}
