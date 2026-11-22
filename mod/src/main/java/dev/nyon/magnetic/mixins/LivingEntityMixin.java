package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

import static dev.nyon.magnetic.utils.MixinHelper.ignoreEntityDrops;
import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapMethod(method = "dropAllDeathLoot")
    private void scopeDropAllDeathLoot(
        ServerLevel level,
        DamageSource source,
        Operation<Void> original
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (MagneticCheckKt.isIgnored(self.getType())) {
            // Ignore the items and experience even when an outer scope, such as an
            // interaction or a nearby block break, already attributes drops to a player.
            ThreadLocalScope.run(ignoreEntityDrops, true, () -> original.call(level, source));
            return;
        }

        if (!(self.getKillCredit() instanceof ServerPlayer player)) {
            original.call(level, source);
            return;
        }

        if (MagneticCheckKt.failsLongRangeCheck(self, player)) {
            ThreadLocalScope.run(ignoreEntityDrops, true, () -> original.call(level, source));
            return;
        }

        ThreadLocalScope.run(threadLocal, player, () -> original.call(level, source));
    }
}
