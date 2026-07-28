package dev.nyon.magnetic.config

import dev.nyon.magnetic.extensions.IdentifierSerializer
import dev.nyon.magnetic.extensions.MinecraftIdentifier
import kotlinx.serialization.Serializable

@Serializable(with = IdentifierSerializer::class)
data class Identifier(val original: MinecraftIdentifier, val isTag: Boolean) {
    override fun toString(): String {
        return "${if (isTag) "#" else ""}$original"
    }
}
