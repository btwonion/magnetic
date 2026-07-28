package dev.nyon.magnetic.config

import dev.nyon.magnetic.extensions.MinecraftIdentifier
import dev.nyon.magnetic.extensions.magneticIdentifier
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey

var ignoredEntities: Set<MinecraftIdentifier> = setOf()

private val registryAccess by lazy { RegistryAccess.ImmutableRegistryAccess(listOf(BuiltInRegistries.ENTITY_TYPE)) }
private val registry by lazy {
    /*? if >=1.21.11 {*/ registryAccess.lookupOrThrow(Registries.ENTITY_TYPE) /*?} else {*/
    /*registryAccess.registryOrThrow(Registries.ENTITY_TYPE) *//*?}*/
}

internal fun reloadIgnoredEntities() {
    val ignored: MutableSet<MinecraftIdentifier> = mutableSetOf()
    config.ignoreEntities.forEach { (original, isTag) ->
        if (!isTag) ignored.add(original)
        else ignored.addAll(original.getTagEntries())
    }
    ignoredEntities = ignored
}

private fun MinecraftIdentifier.getTagEntries(): List<MinecraftIdentifier> {
    val tagKey = TagKey.create(Registries.ENTITY_TYPE, this)
    val entries = registry.getTagOrEmpty(tagKey)
    return entries.map { it.unwrapKey().get().magneticIdentifier() }
}
