package dev.nyon.magnetic.config

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey

val config: Config by lazy {
    config(FabricLoader.getInstance().configDir.resolve("magnetic.json"), 2, Config()) { _, element, version ->
        migrate(
            element, version
        )
    }
    loadConfig()
}
var ignoredEntities: Set<ResourceLocation> = setOf()

@Serializable
data class Config(
    var needEnchantment: Boolean = true,
    var needSneak: Boolean = false,
    var expAllowed: Boolean = true,
    var itemsAllowed: Boolean = true,
    var needPermission: Boolean = false,
    var ignoreKilledEntities: List<Identifier> = listOf()
)

private fun migrate(jsonElement: JsonElement, version: Int?): Config? {
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

private val registryAccess by lazy { RegistryAccess.ImmutableRegistryAccess(listOf(BuiltInRegistries.ENTITY_TYPE)) }
private val registry by lazy { registryAccess.lookupOrThrow(Registries.ENTITY_TYPE) }
internal fun loadIgnoredEntities(): Set<ResourceLocation> {
    val ignored: MutableSet<ResourceLocation> = mutableSetOf()
    config.ignoreKilledEntities.forEach { (original, isTag) ->
        if (!isTag) ignored.add(original)
        else ignored.addAll(original.getTagEntries())
    }
    return ignored
}

private fun ResourceLocation.getTagEntries(): List<ResourceLocation> {
    val tagKey = TagKey.create(Registries.ENTITY_TYPE, this)
    val entries = registry.getTagOrEmpty(tagKey)
    return entries.map { it.unwrapKey().get().location() }
}