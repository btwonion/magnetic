package dev.nyon.magnetic.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.EnchantmentTags
import net.minecraft.world.item.enchantment.Enchantment
import java.util.concurrent.CompletableFuture

class EnchantmentTagProvider(
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