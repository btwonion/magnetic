package dev.nyon.magnetic

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.EnchantmentTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition
import net.minecraft.world.item.enchantment.Enchantment.dynamicCost
import java.util.concurrent.CompletableFuture

val magneticEffectId: TagKey<Enchantment> =
    TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("magnetic", "auto_move"))
val magneticEnchantmentId: ResourceLocation = ResourceLocation.fromNamespaceAndPath("magnetic", "magnetic")

class MagneticEnchantmentGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()

        pack.addProvider(::EnchantmentProvider)
        pack.addProvider(::MagneticEnchantmentTagProvider)
    }
}

private class MagneticEnchantmentTagProvider(
    output: FabricDataOutput, completableFuture: CompletableFuture<HolderLookup.Provider>
) : FabricTagProvider<Enchantment>(output, Registries.ENCHANTMENT, completableFuture) {
    override fun addTags(registries: HolderLookup.Provider) {
        val enchantmentResourceKey = ResourceKey.create(Registries.ENCHANTMENT, magneticEnchantmentId)
        listOf(
            builder(magneticEffectId),
            builder(EnchantmentTags.TRADEABLE),
            builder(EnchantmentTags.IN_ENCHANTING_TABLE),
            builder(EnchantmentTags.TREASURE)
        ).forEach {
            it.addOptional(enchantmentResourceKey)
        }
    }
}

private class EnchantmentProvider(
    output: FabricDataOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>
) : FabricDynamicRegistryProvider(output, registriesFuture) {
    override fun getName(): String {
        return "Magnetic Enchantment Generation"
    }

    override fun configure(registries: HolderLookup.Provider, entries: Entries) {
        val enchantmentDefinition: EnchantmentDefinition = Enchantment.definition(
            registries.lookupOrThrow(Registries.ITEM).getOrThrow(ConventionalItemTags.TOOLS),
            2,
            1,
            dynamicCost(25, 25),
            dynamicCost(75, 75),
            7,
            EquipmentSlotGroup.HAND
        )

        val enchantment = Enchantment.enchantment(enchantmentDefinition).build(
            ResourceLocation.fromNamespaceAndPath("magnetic", "magnetic.name")
        )
        entries.add(ResourceKey.create(Registries.ENCHANTMENT, magneticEnchantmentId), enchantment)
    }
}