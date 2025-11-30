package dev.nyon.magnetic.mixins;

import com.google.gson.JsonElement;
import com.mojang.serialization.Decoder;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.datagen.DataGeneratorKt;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {

    @Inject(
        method = "loadElementFromResource",
        at = @At("HEAD"),
        cancellable = true
    )
    private static <E> void cancelMagneticEnchantmentRegister(
        WritableRegistry<E> registry,
        Decoder<E> decoder,
        RegistryOps<JsonElement> ops,
        ResourceKey<E> registryKey,
        Resource resource,
        RegistrationInfo info,
        CallbackInfo ci
    ) {
        if (!ConfigKt.getConfig()
            .getConditionStatement()
            .getRaw()
            .contains("ENCHANTMENT") && registryKey.location()
            .equals(DataGeneratorKt.getMagneticEnchantmentId())) ci.cancel();
    }
}
