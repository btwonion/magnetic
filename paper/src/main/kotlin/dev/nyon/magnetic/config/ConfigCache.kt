package dev.nyon.magnetic.config

import dev.nyon.magnetic.common.config.Identifier
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed
import org.bukkit.NamespacedKey

@Volatile
private var ignoredEntitiesCache: Set<NamespacedKey>? = null

@Volatile
private var ignoredBlocksCache: Set<NamespacedKey>? = null

val ignoredEntities: Set<NamespacedKey>
    get() = ignoredEntitiesCache ?: resolve(config.ignoreEntities, RegistryKey.ENTITY_TYPE).also {
        ignoredEntitiesCache = it
    }

val ignoredBlocks: Set<NamespacedKey>
    get() = ignoredBlocksCache ?: resolve(config.ignoreBlocks, RegistryKey.BLOCK).also {
        ignoredBlocksCache = it
    }

internal fun invalidateIgnoredCaches() {
    ignoredEntitiesCache = null
    ignoredBlocksCache = null
}

fun Identifier.toNamespacedKey() = NamespacedKey(namespace, path)

private fun <T : Keyed> resolve(entries: List<Identifier>, registryKey: RegistryKey<T>): Set<NamespacedKey> {
    val registry = RegistryAccess.registryAccess().getRegistry(registryKey)
    return entries.flatMapTo(mutableSetOf()) { entry ->
        val key = entry.toNamespacedKey()
        if (!entry.isTag) return@flatMapTo listOf(key)
        registry.getTag(TagKey.create(registryKey, key)).values().map { NamespacedKey(it.key().namespace(), it.key().value()) }
    }
}
