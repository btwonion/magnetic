package dev.nyon.magnetic.mixins.compat;

/*? if fabric {*/
import net.fabricmc.loader.api.FabricLoader;
/*?} else if >=1.21.9 {*/
/*import net.neoforged.fml.loading.FMLLoader;
*//*?} else {*/
/*import net.neoforged.fml.loading.LoadingModList;
*//*?}*/
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class CompatMixinPlugin implements IMixinConfigPlugin {

    static String modIdForMixin(String mixinClassName) {
        String compatPackage = "dev.nyon.magnetic.mixins.compat.";
        if (!mixinClassName.startsWith(compatPackage)) return null;

        String integration = mixinClassName.substring(compatPackage.length()).split("\\.", 2)[0];
        return switch (integration) {
            case "rightclickharvest" -> "rightclickharvest";
            case "veinminer" -> "veinminer";
            case "fallingtree" -> "fallingtree";
            case "kleeslabs" -> "kleeslabs";
            case "treeharvester" -> "treeharvester";
            default -> null;
        };
    }

    static boolean shouldApply(String mixinClassName, Predicate<String> isModLoaded) {
        String modId = modIdForMixin(mixinClassName);
        return modId != null && isModLoaded.test(modId);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return shouldApply(
            mixinClassName,
            /*? if fabric {*/ FabricLoader.getInstance()::isModLoaded
            /*?} else if >=1.21.9 {*/ /*modId -> FMLLoader.getCurrent().getLoadingModList().getModFileById(modId) != null
            *//*?} else {*/ /*modId -> LoadingModList.get().getModFileById(modId) != null *//*?}*/
        );
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
        String targetClassName,
        ClassNode targetClass,
        String mixinClassName,
        IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
        String targetClassName,
        ClassNode targetClass,
        String mixinClassName,
        IMixinInfo mixinInfo
    ) {
    }
}
