package dev.nyon.magnetic.config

import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation

@Serializable(with = IdentifierSerializer::class)
data class Identifier(val original: ResourceLocation, val isTag: Boolean) {
    override fun toString(): String {
        return "${if (isTag) "#" else ""}$original"
    }
}