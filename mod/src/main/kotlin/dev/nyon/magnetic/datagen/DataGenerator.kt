package dev.nyon.magnetic.datagen

/*? if fabric {*/
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class DataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()
        pack.addProvider(::EnchantmentProvider)
        pack.addProvider(::EnchantmentTagProvider)
    }
}
/*?}*/
