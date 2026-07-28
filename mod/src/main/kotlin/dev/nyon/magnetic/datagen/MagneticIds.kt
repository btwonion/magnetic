package dev.nyon.magnetic.datagen

import dev.nyon.magnetic.extensions.MinecraftIdentifier
import dev.nyon.magnetic.extensions.minecraftIdentifier
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.enchantment.Enchantment

val magneticEffectId: TagKey<Enchantment> =
    TagKey.create(Registries.ENCHANTMENT, minecraftIdentifier("magnetic", "auto_move"))
val magneticEnchantmentId: MinecraftIdentifier =
    minecraftIdentifier("magnetic", "magnetic")
