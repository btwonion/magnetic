package dev.nyon.magnetic.config

import dev.nyon.magnetic.extensions.IdentifierSerializer
import kotlinx.serialization.Serializable
import org.bukkit.NamespacedKey

@Serializable(with = IdentifierSerializer::class)
data class Identifier(val original: NamespacedKey, val isTag: Boolean) {
    override fun toString(): String {
        return "${if (isTag) "#" else ""}$original"
    }
}