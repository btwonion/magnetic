package dev.nyon.magnetic.config

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.tags.EntityTypeTagKeys
import org.bukkit.NamespacedKey

var ignoredEntities: Set<NamespacedKey> = setOf()

private val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE)
internal fun reloadIgnoredEntities() {
    val ignored: MutableSet<NamespacedKey> = mutableSetOf()
    config.ignoreEntities.forEach { (original, isTag) ->
        if (!isTag) {
            ignored.add(original)
            return@forEach
        }

        val tagKey = EntityTypeTagKeys.create(original)
        val tag = registry.getTag(tagKey)
        ignored.addAll(tag.values().map { NamespacedKey(it.namespace(), it.value()) })
    }
    ignoredEntities = ignored
}