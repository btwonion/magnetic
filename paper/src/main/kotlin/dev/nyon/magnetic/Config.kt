package dev.nyon.magnetic

import dev.nyon.magnetic.extensions.Identifier
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.tags.EntityTypeTagKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bukkit.NamespacedKey

lateinit var config: Config
var ignoredEntities: Set<NamespacedKey> = setOf()

@Serializable
data class Config(
    var needEnchantment: Boolean = true,
    var needSneak: Boolean = false,
    var expAllowed: Boolean = true,
    var itemsAllowed: Boolean = true,
    var needPermission: Boolean = false,
    var ignoreKilledEntities: List<Identifier> = listOf()
)

internal fun migrate(jsonElement: JsonElement, version: Int?): Config? {
    val jsonObject = jsonElement.jsonObject
    return when (version) {
        1 -> Config(
            needEnchantment = jsonObject["needEnchantment"]?.jsonPrimitive?.boolean ?: return null,
            needSneak = jsonObject["needSneak"]?.jsonPrimitive?.boolean ?: return null,
            expAllowed = jsonObject["expAllowed"]?.jsonPrimitive?.boolean ?: return null,
            itemsAllowed = jsonObject["itemsAllowed"]?.jsonPrimitive?.boolean ?: return null
        )
        else -> null
    }
}

private val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE)
internal fun reloadIgnoredEntities() {
    val ignored: MutableSet<NamespacedKey> = mutableSetOf()
    config.ignoreKilledEntities.forEach { (original, isTag) ->
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