package dev.nyon.magnetic

import kotlinx.serialization.Serializable

lateinit var config: Config

@Serializable
data class Config(
    var needEnchantment: Boolean = true,
    var needSneak: Boolean = false,
    var expAllowed: Boolean = true,
    var itemsAllowed: Boolean = true
)