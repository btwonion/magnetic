package dev.nyon.magnetic.datagen

/*? if fabric {*/
/*? if >=1.21.11 {*/
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider
import net.minecraft.data.tags.TagAppender
/*?} else {*/
/*import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider*//*?}*/
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.EnchantmentTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.enchantment.Enchantment
import java.util.concurrent.CompletableFuture

class EnchantmentTagProvider(
    output: /*? if >=1.21.11 {*/ FabricPackOutput /*?} else {*/ /*FabricDataOutput *//*?}*/,
    completableFuture: CompletableFuture<HolderLookup.Provider>
) : /*? if >=1.21.11 {*/ FabricTagsProvider /*?} else {*/ /*FabricTagProvider *//*?}*/<Enchantment>(output, Registries.ENCHANTMENT, completableFuture) {
    override fun addTags(registries: HolderLookup.Provider) {
        val enchantmentResourceKey = ResourceKey.create(Registries.ENCHANTMENT, magneticEnchantmentId)
        listOf(
            tagBuilder(magneticEffectId),
            tagBuilder(EnchantmentTags.TRADEABLE),
            tagBuilder(EnchantmentTags.IN_ENCHANTING_TABLE),
            tagBuilder(EnchantmentTags.TREASURE)
        ).forEach { it.addOptional(enchantmentResourceKey) }
    }

    private fun tagBuilder(key: TagKey<Enchantment>): /*? if >= 26.2 {*/ TagAppender<Enchantment> /*?} else if >=26.1.2 {*/ /*TagAppender<ResourceKey<Enchantment>, Enchantment> *//*?} else {*/ /*FabricTagBuilder *//*?}*/ {
        return /*? if >=1.21.11 {*/ builder(key) /*?} else {*/ /*getOrCreateTagBuilder(key) *//*?}*/
    }
}
/*?}*/
