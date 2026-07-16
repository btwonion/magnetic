package dev.nyon.magnetic.datagen

/*? if fabric {*/
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class DataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()
        /*? if >=1.21.11 {*/
        pack.addProvider(::EnchantmentProvider)
        pack.addProvider(::EnchantmentTagProvider)
        /*?} else {*/
        /*pack.addProvider { output, registriesFuture -> EnchantmentProvider(output, registriesFuture) }
        pack.addProvider { output, registriesFuture -> EnchantmentTagProvider(output, registriesFuture) }*//*?}*/
    }
}
/*?}*/
