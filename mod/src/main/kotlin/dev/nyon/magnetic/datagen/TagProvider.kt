package dev.nyon.magnetic.datagen

/*? if fabric {*/
/*? if >=26.1.2 {*/
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider
/*?} else {*/
/*import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider*//*?}*/
/*? if >=1.21.11 {*/
import net.minecraft.data.tags.TagAppender
/*?}*/
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.EnchantmentTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.enchantment.Enchantment
import java.util.concurrent.CompletableFuture

class EnchantmentTagProvider(
    output: /*? if >=26.1.2 {*/ FabricPackOutput /*?} else {*/ /*FabricDataOutput *//*?}*/,
    completableFuture: CompletableFuture<HolderLookup.Provider>
) : /*? if >=26.1.2 {*/ FabricTagsProvider /*?} else {*/ /*FabricTagProvider *//*?}*/<Enchantment>(output, Registries.ENCHANTMENT, completableFuture) {
    override fun addTags(registries: HolderLookup.Provider) {
        val enchantmentResourceKey = ResourceKey.create(Registries.ENCHANTMENT, magneticEnchantmentId)
        listOf(
            tagBuilder(magneticEffectId),
            tagBuilder(EnchantmentTags.TRADEABLE),
            tagBuilder(EnchantmentTags.TRADES_DESERT_COMMON),
            tagBuilder(EnchantmentTags.IN_ENCHANTING_TABLE),
            tagBuilder(EnchantmentTags.TREASURE)
        ).forEach { it.addOptional(enchantmentResourceKey) }
    }

    private fun tagBuilder(key: TagKey<Enchantment>): /*? if >= 26.2 {*/ TagAppender<Enchantment> /*?} else if >=1.21.11 {*/ /*TagAppender<ResourceKey<Enchantment>, Enchantment> *//*?} else {*/ /*FabricTagBuilder *//*?}*/ {
        return /*? if >=1.21.11 {*/ builder(key) /*?} else {*/ /*getOrCreateTagBuilder(key) *//*?}*/
    }
}
/*?}*/
