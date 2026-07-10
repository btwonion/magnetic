package dev.nyon.magnetic.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.EnchantmentTags
import net.minecraft.world.item.enchantment.Enchantment
import java.util.concurrent.CompletableFuture

class EnchantmentTagProvider(
    output: FabricPackOutput, completableFuture: CompletableFuture<HolderLookup.Provider>
) : FabricTagsProvider<Enchantment>(output, Registries.ENCHANTMENT, completableFuture) {
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