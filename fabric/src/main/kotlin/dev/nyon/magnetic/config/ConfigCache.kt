package dev.nyon.magnetic.config

import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey

var ignoredEntities: Set<Identifier> = setOf()

private val registryAccess by lazy { RegistryAccess.ImmutableRegistryAccess(listOf(BuiltInRegistries.ENTITY_TYPE)) }
private val registry by lazy { registryAccess.lookupOrThrow(Registries.ENTITY_TYPE) }

internal fun reloadIgnoredEntities() {
    val ignored: MutableSet<Identifier> = mutableSetOf()
    config.ignoreEntities.forEach { (original, isTag) ->
        if (!isTag) ignored.add(original)
        else ignored.addAll(original.getTagEntries())
    }
    ignoredEntities = ignored
}

private fun Identifier.getTagEntries(): List<Identifier> {
    val tagKey = TagKey.create(Registries.ENTITY_TYPE, this)
    val entries = registry.getTagOrEmpty(tagKey)
    return entries.map { it.unwrapKey().get().identifier() }
}