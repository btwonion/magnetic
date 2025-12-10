package dev.nyon.magnetic.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition
import net.minecraft.world.item.enchantment.Enchantment.dynamicCost
import java.util.concurrent.CompletableFuture

class EnchantmentProvider(
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
            Identifier.fromNamespaceAndPath("magnetic", "magnetic.name")
        )
        entries.add(ResourceKey.create(Registries.ENCHANTMENT, magneticEnchantmentId), enchantment)
    }
}