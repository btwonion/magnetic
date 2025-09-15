package dev.nyon.magnetic.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

lateinit var config: Config

@Serializable
data class Config(
    var enchantmentRequired: Boolean = true,
    var sneakRequired: Boolean = false,
    var permissionRequired: Boolean = false,
    var itemsAllowed: Boolean = true,
    var expAllowed: Boolean = true,
    var ignoreRangedWeapons: Boolean = true,
    var ignoreEntities: List<Identifier> = listOf(),
    var fullInventoryAlert: FullInventoryAlert = FullInventoryAlert()
) {
    @Serializable
    data class FullInventoryAlert(
        var soundAlert: SoundAlert = SoundAlert(), var textAlert: TextAlert = TextAlert()
    ) {
        interface Alert {
            var enabled: Boolean
            var cooldownInSeconds: Int

            fun invoke(player: Player)
        }

        @Serializable
        data class SoundAlert(
            override var enabled: Boolean = true, override var cooldownInSeconds: Int = 5
        ) : Alert {
            @Transient
            private val sound = Sound.sound(Key.key("block.note_block.pling"), Sound.Source.MASTER, 1f, 1f)

            override fun invoke(player: Player) {
                player.playSound(sound)
            }
        }

        @Serializable
        data class TextAlert(
            override var enabled: Boolean = true, override var cooldownInSeconds: Int = 60
        ) : Alert {
            override fun invoke(player: Player) {
                player.sendMessage(
                    Component.translatable("chat.message.fullinventoryalert.text")
                )
            }
        }

    }
}

internal fun migrate(jsonElement: JsonElement, version: Int?): Config? {
    val jsonObject = jsonElement.jsonObject
    return when (version) {
        1 -> Config(
            enchantmentRequired = jsonObject["needEnchantment"]?.jsonPrimitive?.boolean ?: return null,
            sneakRequired = jsonObject["needSneak"]?.jsonPrimitive?.boolean ?: return null,
            itemsAllowed = jsonObject["itemsAllowed"]?.jsonPrimitive?.boolean ?: return null,
            expAllowed = jsonObject["expAllowed"]?.jsonPrimitive?.boolean ?: return null
        )
        else -> null
    }
}
