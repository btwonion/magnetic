package dev.nyon.magnetic.config

import dev.nyon.konfig.config.config
import dev.nyon.konfig.config.loadConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

val config: Config by lazy {
    config(FabricLoader.getInstance().configDir.resolve("magnetic.json"), 3, Config()) { _, element, version ->
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

            fun invoke(player: ServerPlayer)
        }

        @Serializable
        data class SoundAlert(
            override var enabled: Boolean = true, override var cooldownInSeconds: Int = 5
        ) : Alert {
            private val sound by lazy {
                SoundEvents.NOTE_BLOCK_PLING.value()
            }

            override fun invoke(player: ServerPlayer) {
                player.playNotifySound(sound, SoundSource.MASTER, 1f, 1f)
            }
        }

        @Serializable
        data class TextAlert(
            override var enabled: Boolean = true, override var cooldownInSeconds: Int = 60
        ) : Alert {
            override fun invoke(player: ServerPlayer) {
                player.sendSystemMessage(
                    Component.translatable("chat.message.fullinventoryalert.text").withStyle(ChatFormatting.GOLD)
                )
            }
        }

        @Serializable
        data class TitleAlert(
            override var enabled: Boolean = true, override var cooldownInSeconds: Int = 5
        ) : Alert {
            override fun invoke(player: ServerPlayer) {
                val titlePacket = ClientboundSetTitleTextPacket(
                    Component.translatable("chat.message.fullinventoryalert.title.main").withStyle(
                        ChatFormatting.RED
                    )
                )
                val subtitlePacket = ClientboundSetSubtitleTextPacket(
                    Component.translatable("chat.message.fullinventoryalert.title.subtitle").withStyle(
                        ChatFormatting.RED
                    )
                )

                player.connection.send(titlePacket)
                player.connection.send(subtitlePacket)
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
        else -> null
    }
}