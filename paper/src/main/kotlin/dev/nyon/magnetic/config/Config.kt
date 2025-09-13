package dev.nyon.magnetic.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

lateinit var config: Config

@Serializable
data class Config(
    var enchantmentRequired: Boolean = true,
    var sneakRequired: Boolean = false,
    var permissionRequired: Boolean = false,
    var itemsAllowed: Boolean = true,
    var expAllowed: Boolean = true,
    var ignoreRangedWeapons: Boolean = true,
    var ignoreEntities: List<Identifier> = listOf()
)

internal fun migrate(jsonElement: JsonElement, version: Int?): Config? {
    val jsonObject = jsonElement.jsonObject
    return when (version) {
        1 -> Config(
            enchantmentRequired = jsonObject["needEnchantment"]?.jsonPrimitive?.boolean ?: return null,
            sneakRequired = jsonObject["needSneak"]?.jsonPrimitive?.boolean ?: return null,
            itemsAllowed = jsonObject["itemsAllowed"]?.jsonPrimitive?.boolean ?: return null,
            expAllowed = jsonObject["expAllowed"]?.jsonPrimitive?.boolean ?: return null
        )
        else -> null
    }
}