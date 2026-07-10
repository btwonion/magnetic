@file:Suppress("unused")

package dev.nyon.magnetic

/*? if neoforge {*//*
import dev.nyon.magnetic.config.screen.generateConfigScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.gui.IConfigScreenFactory

@Mod(value = "magnetic", dist = [Dist.CLIENT])
object MagneticClientEntrypoint {
    init {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _, parent -> generateConfigScreen(parent) }
        }
    }
}
*//*?}*/
