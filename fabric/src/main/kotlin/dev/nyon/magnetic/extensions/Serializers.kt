package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.Identifier
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.resources.ResourceLocation

object IdentifierSerializer : KSerializer<Identifier> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("identifier", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Identifier {
        return decodeFromString(decoder.decodeString())
    }

    fun decodeFromString(string: String): Identifier {
        val isTag = string.startsWith('#')
        val namespacedKey = ResourceLocation.parse(string.run { return@run if (isTag) drop(1) else this@run }) ?: error(
            "Magnetic couldn't parse malformed identifier: '$string'."
        )
        return Identifier(namespacedKey, isTag)
    }

    override fun serialize(
        encoder: Encoder, value: Identifier
    ) {
        encoder.encodeString(value.toString())
    }
}