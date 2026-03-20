package dev.nyon.magnetic.mixins;

import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.datagen.DataGeneratorKt;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistryLoadTask.class)
public class RegistryLoadTaskMixin {

    @Inject(
        method = "lambda$registerElements$0",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void cancelMagneticEnchantmentRegister(
        RegistryLoadTask.PendingRegistration element,
        CallbackInfo ci
    ) {
        ResourceKey key = element.key();
        if (!ConfigKt.getConfig()
            .getConditionStatement()
            .getRaw()
            .contains("ENCHANTMENT") && key.identifier()
            .equals(DataGeneratorKt.getMagneticEnchantmentId())) ci.cancel();
    }
}
