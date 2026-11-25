package dev.nyon.magnetic.config

import dev.nyon.magnetic.common.config.Identifier
import dev.nyon.magnetic.extensions.MinecraftIdentifier
import dev.nyon.magnetic.extensions.magneticIdentifier
import dev.nyon.magnetic.extensions.toMinecraft
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey

private var ignoredEntitiesCache: Set<MinecraftIdentifier>? = null
private var ignoredBlocksCache: Set<MinecraftIdentifier>? = null

val ignoredEntities: Set<MinecraftIdentifier>
    get() = ignoredEntitiesCache ?: resolve(config.ignoreEntities, BuiltInRegistries.ENTITY_TYPE).also {
        ignoredEntitiesCache = it
    }

val ignoredBlocks: Set<MinecraftIdentifier>
    get() = ignoredBlocksCache ?: resolve(config.ignoreBlocks, BuiltInRegistries.BLOCK).also {
        ignoredBlocksCache = it
    }

internal fun invalidateIgnoredCaches() {
    ignoredEntitiesCache = null
    ignoredBlocksCache = null
}

private fun <T : Any> resolve(entries: List<Identifier>, registry: Registry<T>): Set<MinecraftIdentifier> =
    entries.flatMapTo(mutableSetOf()) { entry ->
        val id = entry.toMinecraft()
        if (!entry.isTag) return@flatMapTo listOf(id)
        registry.getTagOrEmpty(TagKey.create(registry.key(), id)).map { it.unwrapKey().get().magneticIdentifier() }
    }
