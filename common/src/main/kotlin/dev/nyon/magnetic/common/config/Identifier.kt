package dev.nyon.magnetic.common.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = IdentifierSerializer::class)
data class Identifier(val namespace: String, val path: String, val isTag: Boolean = false) {
    override fun toString(): String = "${if (isTag) "#" else ""}$namespace:$path"

    companion object {
        private val namespacePattern = Regex("[a-z0-9_.-]+")
        private val pathPattern = Regex("[a-z0-9_./-]+")

        fun parse(value: String): Identifier {
            val isTag = value.startsWith('#')
            val id = value.removePrefix("#")
            val namespace = id.substringBefore(':', "minecraft")
            val path = id.substringAfter(':')
            require(namespacePattern.matches(namespace) && pathPattern.matches(path)) {
                "Magnetic couldn't parse malformed identifier: '$value'."
            }
            return Identifier(namespace, path, isTag)
        }
    }
}

object IdentifierSerializer : KSerializer<Identifier> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("identifier", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Identifier = Identifier.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Identifier) = encoder.encodeString(value.toString())
}
