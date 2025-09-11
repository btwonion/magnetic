package dev.nyon.magnetic.extensions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.NamespacedKey

object IdentifierSerializer : KSerializer<Identifier> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("identifier", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Identifier {
        val rawString = decoder.decodeString()
        val isTag = rawString.startsWith('#')
        val namespacedKey = NamespacedKey.fromString(rawString.drop(1)) ?: error("Magnetic couldn't parse malformed identifier: '$rawString'.")
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