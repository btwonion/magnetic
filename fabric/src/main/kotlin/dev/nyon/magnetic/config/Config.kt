package dev.nyon.magnetic.config

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.fabricmc.loader.api.FabricLoader

val config: Config by lazy {
    config(FabricLoader.getInstance().configDir.resolve("magnetic.json"), 2, Config()) { _, element, version ->
        migrate(
            element,
            version
        )
    }
    loadConfig()
}

@Serializable
data class Config(
    var needEnchantment: Boolean = true,
    var needSneak: Boolean = false,
    var expAllowed: Boolean = true,
    var itemsAllowed: Boolean = true,
    var needPermission: Boolean = false
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