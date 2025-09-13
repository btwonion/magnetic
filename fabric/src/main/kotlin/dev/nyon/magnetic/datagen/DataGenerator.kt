package dev.nyon.magnetic.datagen

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.enchantment.Enchantment

val magneticEffectId: TagKey<Enchantment> =
    TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("magnetic", "auto_move"))
val magneticEnchantmentId: ResourceLocation = ResourceLocation.fromNamespaceAndPath("magnetic", "magnetic")

class DataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()

        pack.addProvider(::EnchantmentProvider)
        pack.addProvider(::EnchantmentTagProvider)
        pack.addProvider(::ItemTagProvider)
    }
}