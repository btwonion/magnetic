package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.Identifier
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.NamespacedKey

object IdentifierSerializer : KSerializer<Identifier> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("identifier", PrimitiveKind.STRING)

    fun decodeFromString(value: String): Identifier {
        val isTag = value.startsWith('#')
        val namespacedKey = NamespacedKey.fromString(value.run { return@run if (isTag) drop(1) else this@run })
            ?: error("Magnetic couldn't parse malformed identifier: '$value'.")
        return Identifier(namespacedKey, isTag)
    }

    override fun deserialize(decoder: Decoder): Identifier {
        val rawString = decoder.decodeString()
        return decodeFromString(rawString)
    }

    override fun serialize(
        encoder: Encoder, value: Identifier
    ) {
        encoder.encodeString(value.toString())
    }
}