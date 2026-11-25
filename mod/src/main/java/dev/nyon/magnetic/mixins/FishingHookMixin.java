package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Shadow
    @Nullable
    public abstract Player getPlayerOwner();

    @WrapMethod(method = "retrieve")
    private int scopeRetrieveToPlayer(
        ItemStack stack,
        Operation<Integer> original
    ) {
        if (!(getPlayerOwner() instanceof ServerPlayer player)) return original.call(stack);
        return ThreadLocalScope.call(threadLocal, player, () -> original.call(stack));
    }
}
