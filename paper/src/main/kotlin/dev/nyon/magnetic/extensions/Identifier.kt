package dev.nyon.magnetic.extensions

import kotlinx.serialization.Serializable
import org.bukkit.NamespacedKey

@Serializable(with = IdentifierSerializer::class)
data class Identifier(val original: NamespacedKey, val isTag: Boolean)