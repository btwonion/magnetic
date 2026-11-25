package dev.nyon.magnetic.mixins.compat.puzzleslib;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Pseudo
@Mixin(targets = "fuzs.puzzleslib.fabric.impl.event.FabricEventImplHelper", remap = false)
public class FabricEventImplHelperMixin {

    @WrapMethod(method = "tryOnLivingDrops", remap = false)
    private static boolean scopeCapturedDropsToPlayer(
        LivingEntity entity,
        DamageSource source,
        int lastHurtByPlayerMemoryTime,
        Operation<Boolean> original
    ) {
        if (!(entity.getKillCredit() instanceof ServerPlayer player)
            || MagneticCheckKt.isIgnored(entity.getType())
            || MagneticCheckKt.failsLongRangeCheck(entity, player)) {
            return original.call(entity, source, lastHurtByPlayerMemoryTime);
        }
        return ThreadLocalScope.call(
            threadLocal,
            player,
            () -> original.call(entity, source, lastHurtByPlayerMemoryTime)
        );
    }
}
