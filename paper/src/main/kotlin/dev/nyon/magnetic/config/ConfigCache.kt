package dev.nyon.magnetic.config

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.NamespacedKey

var ignoredEntities: Set<NamespacedKey> = setOf()
var ignoredBlocks: Set<NamespacedKey> = setOf()

private val entityRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE)
private val blockRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK)

internal fun reloadIgnoredEntities() {
    val ignored: MutableSet<NamespacedKey> = mutableSetOf()
    config.ignoreEntities.forEach { (original, isTag) ->
        if (!isTag) {
            ignored.add(original)
            return@forEach
        }

        val tagKey = TagKey.create(RegistryKey.ENTITY_TYPE, original)
        val tag = entityRegistry.getTag(tagKey)
        ignored.addAll(tag.values().map { NamespacedKey(it.key().namespace(), it.key().value()) })
    }
    ignoredEntities = ignored
}

internal fun reloadIgnoredBlocks() {
    val ignored: MutableSet<NamespacedKey> = mutableSetOf()
    config.ignoreBlocks.forEach { (original, isTag) ->
        if (!isTag) {
            ignored.add(original)
            return@forEach
        }

        val tagKey = TagKey.create(RegistryKey.BLOCK, original)
        val tag = blockRegistry.getTag(tagKey)
        ignored.addAll(tag.values().map { NamespacedKey(it.key().namespace(), it.key().value()) })
    }
    ignoredBlocks = ignored
}
