package dev.nyon.magnetic.mixins;

import dev.nyon.magnetic.extensions.MagneticCheckKt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(
        method = "dropAllDeathLoot",
        at = @At("HEAD")
    )
    private void setPlayerOnDropAllDeathLoot(
        ServerLevel level,
        DamageSource source,
        CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (MagneticCheckKt.isIgnored(self.getType())) return;
        if (MagneticCheckKt.failsLongRangeCheck(self, player)) return;
        threadLocal.set(player);
    }

    @Inject(
        method = "dropAllDeathLoot",
        at = @At("RETURN")
    )
    private void clearPlayerOnDropAllDeathLoot(
        ServerLevel level,
        DamageSource source,
        CallbackInfo ci
    ) {
        threadLocal.remove();
    }
}
