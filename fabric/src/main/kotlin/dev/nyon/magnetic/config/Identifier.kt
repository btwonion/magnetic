package dev.nyon.magnetic.config

import dev.nyon.magnetic.extensions.IdentifierSerializer
import kotlinx.serialization.Serializable
import net.minecraft.resources.Identifier

@Serializable(with = IdentifierSerializer::class)
data class Identifier(val original: Identifier, val isTag: Boolean) {
    override fun toString(): String {
        return "${if (isTag) "#" else ""}$original"
    }
}