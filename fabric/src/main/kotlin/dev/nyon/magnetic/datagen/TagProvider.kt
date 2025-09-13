package dev.nyon.magnetic.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.EnchantmentTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import java.util.concurrent.CompletableFuture

val rangedWeaponLocation: ResourceLocation = ResourceLocation.fromNamespaceAndPath("magnetic", "ranged_weapons")
val rangedWeaponKey: TagKey<Item> = TagKey.create(Registries.ITEM, rangedWeaponLocation)

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

class ItemTagProvider(
    output: FabricDataOutput, completableFuture: CompletableFuture<HolderLookup.Provider>
) : FabricTagProvider.ItemTagProvider(output, completableFuture) {
    override fun addTags(registries: HolderLookup.Provider) {
        valueLookupBuilder(rangedWeaponKey)
            .addOptional(Items.BOW)
            .addOptional(Items.CROSSBOW)
            .addOptional(Items.TRIDENT)
    }
}