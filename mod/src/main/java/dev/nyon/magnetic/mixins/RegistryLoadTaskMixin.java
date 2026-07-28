package dev.nyon.magnetic.mixins;

/*? if <26.1.2 {*/
/*import com.google.gson.JsonElement;
import com.mojang.serialization.Decoder;
*//*?}*/
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.datagen.MagneticIdsKt;
/*? if >=26.1.2 {*/
import net.minecraft.resources.RegistryLoadTask;
/*?} else {*/
/*import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.Resource;
*//*?}*/
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(/*? if >=26.1.2 {*/ RegistryLoadTask.class /*?} else {*/ /*RegistryDataLoader.class *//*?}*/)
public class RegistryLoadTaskMixin {

    private static boolean isMagneticKey(ResourceKey key) {
        return /*? if >=1.21.11 {*/ key.identifier() /*?} else {*/ /*key.location() *//*?}*/
            .equals(MagneticIdsKt.getMagneticEnchantmentId());
    }

    /*? if >=26.1.2 {*/
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
            .contains("ENCHANTMENT") && isMagneticKey(key)) ci.cancel();
    }
    /*?} else {*/
    /*@Inject(method = "loadElementFromResource", at = @At("HEAD"), cancellable = true)
    private static <E> void cancelMagneticEnchantmentRegister(
        WritableRegistry<E> registry,
        Decoder<E> decoder,
        RegistryOps<JsonElement> ops,
        ResourceKey<E> registryKey,
        Resource resource,
        RegistrationInfo info,
        CallbackInfo ci
    ) {
        if (!ConfigKt.getConfig().getConditionStatement().getRaw().contains("ENCHANTMENT")
            && isMagneticKey(registryKey)) {
            ci.cancel();
        }
    }
    *//*?}*/
}
