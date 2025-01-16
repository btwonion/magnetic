package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit

val config: Config by lazy {
    config(Bukkit.getPluginsFolder().toPath().resolve("magnetic.json"), 1, Config()) { _, _ -> null }
    loadConfig()
}

@Serializable
data class Config(
    var needEnchantment: Boolean = true,
    var needSneak: Boolean = false,
    var expAllowed: Boolean = true,
    var itemsAllowed: Boolean = true
)