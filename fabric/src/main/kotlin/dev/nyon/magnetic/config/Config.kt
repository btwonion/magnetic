package dev.nyon.magnetic.config

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import kotlinx.serialization.Serializable
import net.fabricmc.loader.api.FabricLoader

val config: Config by lazy {
    config(FabricLoader.getInstance().configDir.resolve("magnetic.json"), 1, Config()) { _, _ -> null }
    loadConfig()
}

@Serializable
data class Config(
    var needEnchantment: Boolean = true,
    var needSneak: Boolean = false,
    var expAllowed: Boolean = true,
    var itemsAllowed: Boolean = true
)