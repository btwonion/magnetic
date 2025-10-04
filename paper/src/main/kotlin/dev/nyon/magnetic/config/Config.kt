package dev.nyon.magnetic.config

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import dev.nyon.magnetic.configPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player

val config: Config by lazy {
    config(configPath, 3, Config()) { _, element, version ->
        migrate(element, version)
    }
    loadConfig()
}

@Serializable
data class Config(
    var enchantmentRequired: Boolean = true,
    var sneakRequired: Boolean = false,
    var permissionRequired: Boolean = false,
    var itemsAllowed: Boolean = true,
    var expAllowed: Boolean = true,
    var ignoreRangedWeapons: Boolean = true,
    var ignoreEntities: List<Identifier> = listOf(),
    var fullInventoryAlert: FullInventoryAlert = FullInventoryAlert(),
    var animation: Animation = Animation()
) {
    @Serializable
    data class FullInventoryAlert(
        var soundAlert: SoundAlert = SoundAlert(),
        var textAlert: TextAlert = TextAlert(),
        var titleAlert: TitleAlert = TitleAlert()
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

        @Serializable
        data class TitleAlert(
            override var enabled: Boolean = true, override var cooldownInSeconds: Int = 5
        ) : Alert {
            override fun invoke(player: Player) {
                player.showTitle(
                    Title.title(
                        Component.translatable("chat.message.fullinventoryalert.title.main"),
                        Component.translatable("chat.message.fullinventoryalert.title.subtitle")
                    )
                )
            }
        }
    }

    @Serializable
    data class Animation(
        var enabled: Boolean = true,
        var blocksPerSecond: Double = 1.5,
        var canOtherPlayersPickup: Boolean = false
    )
}

private fun migrate(jsonElement: JsonElement, version: Int?): Config? {
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