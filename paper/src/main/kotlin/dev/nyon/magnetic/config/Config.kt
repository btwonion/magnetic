package dev.nyon.magnetic.config

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import dev.nyon.magnetic.configPath
import dev.nyon.magnetic.extensions.IdentifierSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player

val config: Config by lazy {
    config(configPath, 4, Config()) { _, element, version ->
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
    var ignoredEntitiesRangeMin: Double = 15.0,
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
        var blocksPerSecond: Double = 1.0,
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
        3 -> {
            val fullInventoryAlertObject = jsonObject["fullInventoryAlert"]?.jsonObject ?: return null
            val animationObject = jsonObject["animation"]?.jsonObject ?: return null

            Config(
                enchantmentRequired = jsonObject["enchantmentRequired"]?.jsonPrimitive?.boolean ?: return null,
                sneakRequired = jsonObject["sneakRequired"]?.jsonPrimitive?.boolean ?: return null,
                permissionRequired = jsonObject["permissionRequired"]?.jsonPrimitive?.boolean ?: return null,
                itemsAllowed = jsonObject["itemsAllowed"]?.jsonPrimitive?.boolean ?: return null,
                expAllowed = jsonObject["expAllowed"]?.jsonPrimitive?.boolean ?: return null,
                ignoredEntitiesRangeMin = if (jsonObject["ignoreRangedWeapons"]?.jsonPrimitive?.boolean ?: return null) 15.0 else -1.0,
                ignoreEntities = jsonObject["ignoreEntities"]?.jsonArray?.map { element -> IdentifierSerializer.decodeFromString(element.jsonPrimitive.content) } ?: return null,
                fullInventoryAlert = Config.FullInventoryAlert(
                    soundAlert = Config.FullInventoryAlert.SoundAlert(
                        enabled = fullInventoryAlertObject["soundAlert"]?.jsonObject["enabled"]?.jsonPrimitive?.boolean ?: return null,
                        cooldownInSeconds = fullInventoryAlertObject["soundAlert"]?.jsonObject["cooldownInSeconds"]?.jsonPrimitive?.int ?: return null
                    ),
                    textAlert = Config.FullInventoryAlert.TextAlert(
                        enabled = fullInventoryAlertObject["textAlert"]?.jsonObject["enabled"]?.jsonPrimitive?.boolean ?: return null,
                        cooldownInSeconds = fullInventoryAlertObject["textAlert"]?.jsonObject["cooldownInSeconds"]?.jsonPrimitive?.int ?: return null
                    ),
                    titleAlert = Config.FullInventoryAlert.TitleAlert(
                        enabled = fullInventoryAlertObject["titleAlert"]?.jsonObject["enabled"]?.jsonPrimitive?.boolean ?: return null,
                        cooldownInSeconds = fullInventoryAlertObject["titleAlert"]?.jsonObject["cooldownInSeconds"]?.jsonPrimitive?.int ?: return null
                    )
                ),
                animation = Config.Animation(
                    enabled = animationObject["enabled"]?.jsonPrimitive?.boolean ?: return null,
                    blocksPerSecond = animationObject["blocksPerSecond"]?.jsonPrimitive?.double ?: return null,
                    canOtherPlayersPickup = animationObject["canOtherPlayersPickup"]?.jsonPrimitive?.boolean ?: return null
                )
            )
        }
        else -> null
    }
}