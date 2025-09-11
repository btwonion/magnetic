package dev.nyon.magnetic.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.resources.ResourceLocation

object ResourceLocationSerializer : KSerializer<Identifier> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("resource_location", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Identifier {
        val rawString = decoder.decodeString()
        val isTag = rawString.startsWith('#')
        val namespacedKey = ResourceLocation.parse(rawString.run { return@run if (isTag) drop(1) else this@run }) ?: error("Magnetic couldn't parse malformed identifier: '$rawString'.")
        return Identifier(namespacedKey, isTag)
    }

    override fun serialize(
        encoder: Encoder, value: Identifier
    ) {
        val rawIdentifier = buildString {
            if (value.isTag) append('#')
            append(value.original.toString())
        }
        encoder.encodeString(rawIdentifier)
    }
}