package dev.nyon.magnetic.datagen

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.enchantment.Enchantment

val magneticEffectId: TagKey<Enchantment> =
    TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("magnetic", "auto_move"))
val magneticEnchantmentId: Identifier =
    Identifier.fromNamespaceAndPath("magnetic", "magnetic")
