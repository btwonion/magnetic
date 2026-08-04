package dev.nyon.magnetic.mixins.compat.puzzleslib;

import dev.nyon.magnetic.extensions.MagneticCheckKt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Pseudo
@Mixin(targets = "fuzs.puzzleslib.fabric.impl.event.FabricEventImplHelper", remap = false)
public class FabricEventImplHelperMixin {

    @Inject(method = "tryOnLivingDrops", at = @At("HEAD"), remap = false)
    private static void setPlayerOnCapturedDrops(
        LivingEntity entity,
        DamageSource source,
        int lastHurtByPlayerMemoryTime,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (MagneticCheckKt.isIgnored(entity.getType())) return;
        if (MagneticCheckKt.failsLongRangeCheck(entity, player)) return;
        threadLocal.set(player);
    }

    @Inject(method = "tryOnLivingDrops", at = @At("RETURN"), remap = false)
    private static void clearPlayerOnCapturedDrops(
        LivingEntity entity,
        DamageSource source,
        int lastHurtByPlayerMemoryTime,
        CallbackInfoReturnable<Boolean> cir
    ) {
        threadLocal.remove();
    }
}
