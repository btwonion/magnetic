package dev.nyon.magnetic.config

import dev.nyon.magnetic.extensions.MinecraftIdentifier
import dev.nyon.magnetic.extensions.magneticIdentifier
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey

var ignoredEntities: Set<MinecraftIdentifier> = setOf()
var ignoredBlocks: Set<MinecraftIdentifier> = setOf()
private var ignoredEntitiesInitialized = false
private var ignoredBlocksInitialized = false

private val registryAccess by lazy {
    RegistryAccess.ImmutableRegistryAccess(listOf(BuiltInRegistries.ENTITY_TYPE, BuiltInRegistries.BLOCK))
}
private val entityRegistry by lazy {
    /*? if >=1.21.11 {*/ registryAccess.lookupOrThrow(Registries.ENTITY_TYPE) /*?} else {*/
    /*registryAccess.registryOrThrow(Registries.ENTITY_TYPE) *//*?}*/
}
private val blockRegistry by lazy {
    /*? if >=1.21.11 {*/ registryAccess.lookupOrThrow(Registries.BLOCK) /*?} else {*/
    /*registryAccess.registryOrThrow(Registries.BLOCK) *//*?}*/
}

internal fun reloadIgnoredEntities() {
    val ignored: MutableSet<MinecraftIdentifier> = mutableSetOf()
    config.ignoreEntities.forEach { (original, isTag) ->
        if (!isTag) ignored.add(original)
        else ignored.addAll(original.getEntityTagEntries())
    }
    ignoredEntities = ignored
    ignoredEntitiesInitialized = true
}

internal fun reloadIgnoredBlocks() {
    val ignored: MutableSet<MinecraftIdentifier> = mutableSetOf()
    config.ignoreBlocks.forEach { (original, isTag) ->
        if (!isTag) ignored.add(original)
        else ignored.addAll(original.getBlockTagEntries())
    }
    ignoredBlocks = ignored
    ignoredBlocksInitialized = true
}

internal fun ensureIgnoredEntitiesLoaded() {
    if (!ignoredEntitiesInitialized) reloadIgnoredEntities()
}

internal fun ensureIgnoredBlocksLoaded() {
    if (!ignoredBlocksInitialized) reloadIgnoredBlocks()
}

internal fun invalidateIgnoredCaches() {
    ignoredEntitiesInitialized = false
    ignoredBlocksInitialized = false
}

private fun MinecraftIdentifier.getEntityTagEntries(): List<MinecraftIdentifier> {
    val tagKey = TagKey.create(Registries.ENTITY_TYPE, this)
    val entries = entityRegistry.getTagOrEmpty(tagKey)
    return entries.map { it.unwrapKey().get().magneticIdentifier() }
}

private fun MinecraftIdentifier.getBlockTagEntries(): List<MinecraftIdentifier> {
    val tagKey = TagKey.create(Registries.BLOCK, this)
    val entries = blockRegistry.getTagOrEmpty(tagKey)
    return entries.map { it.unwrapKey().get().magneticIdentifier() }
}
