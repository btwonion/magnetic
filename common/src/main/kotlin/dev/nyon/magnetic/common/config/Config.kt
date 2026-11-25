package dev.nyon.magnetic.common.config

import dev.nyon.magnetic.common.conditions.ConditionChain
import dev.nyon.magnetic.common.conditions.ConditionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

const val CONFIG_VERSION = 6

@Serializable
data class Config(
    var conditionStatement: ConditionChain = ConditionChain("ENCHANTMENT"),
    var itemsAllowed: Boolean = true,
    var expAllowed: Boolean = true,
    var buckets: Buckets = Buckets(),
    var leafDecay: LeafDecay = LeafDecay(),
    var ignoredEntitiesRangeMin: Double = 50.0,
    var ignoreEntities: List<Identifier> = listOf(Identifier("minecraft", "ender_dragon")),
    var ignoreBlocks: List<Identifier> = listOf(),
    var fullInventoryAlert: FullInventoryAlert = FullInventoryAlert(),
    var animation: Animation = Animation()
) {
    @Serializable
    data class Buckets(
        var enabled: Boolean = true,
        var abilityTimeout: Long = 30000
    )

    @Serializable
    data class LeafDecay(
        var enabled: Boolean = false,
        var abilityTimeout: Long = 300000
    )

    @Serializable
    data class FullInventoryAlert(
        var soundAlert: SoundAlert = SoundAlert(),
        var textAlert: TextAlert = TextAlert(),
        var titleAlert: TitleAlert = TitleAlert()
    ) {
        interface Alert {
            var enabled: Boolean
            var cooldownInSeconds: Int
        }

        @Serializable
        data class SoundAlert(
            override var enabled: Boolean = true,
            override var cooldownInSeconds: Int = 5
        ) : Alert

        @Serializable
        data class TextAlert(
            override var enabled: Boolean = true,
            override var cooldownInSeconds: Int = 60
        ) : Alert

        @Serializable
        data class TitleAlert(
            override var enabled: Boolean = false,
            override var cooldownInSeconds: Int = 5
        ) : Alert
    }

    @Serializable
    data class Animation(
        var enabled: Boolean = true,
        var blocksPerSecond: Double = 1.0,
        var canOtherPlayersPickup: Boolean = false
    )
}

fun migrate(jsonElement: JsonElement, version: Int?): Config? {
    val jsonObject = jsonElement.jsonObject
    fun JsonObject.boolean(key: String) = get(key)?.jsonPrimitive?.boolean
    fun JsonObject.int(key: String) = get(key)?.jsonPrimitive?.int
    fun JsonObject.child(key: String) = get(key)?.jsonObject

    return when (version) {
        1 -> Config(
            conditionStatement = legacyConditionStatement(
                jsonObject.boolean("needEnchantment") ?: return null,
                jsonObject.boolean("needSneak") ?: return null
            ),
            itemsAllowed = jsonObject.boolean("itemsAllowed") ?: return null,
            expAllowed = jsonObject.boolean("expAllowed") ?: return null
        )
        3 -> {
            val alerts = jsonObject.child("fullInventoryAlert") ?: return null
            val soundAlert = alerts.child("soundAlert") ?: return null
            val textAlert = alerts.child("textAlert") ?: return null
            val titleAlert = alerts.child("titleAlert") ?: return null
            val animation = jsonObject.child("animation") ?: return null

            Config(
                conditionStatement = legacyConditionStatement(
                    jsonObject.boolean("enchantmentRequired") ?: return null,
                    jsonObject.boolean("sneakRequired") ?: return null
                ),
                itemsAllowed = jsonObject.boolean("itemsAllowed") ?: return null,
                expAllowed = jsonObject.boolean("expAllowed") ?: return null,
                ignoredEntitiesRangeMin = if (jsonObject.boolean("ignoreRangedWeapons") ?: return null) 15.0 else -1.0,
                ignoreEntities = jsonObject["ignoreEntities"]?.jsonArray?.map { Identifier.parse(it.jsonPrimitive.content) }
                    ?: return null,
                fullInventoryAlert = Config.FullInventoryAlert(
                    Config.FullInventoryAlert.SoundAlert(
                        soundAlert.boolean("enabled") ?: return null,
                        soundAlert.int("cooldownInSeconds") ?: return null
                    ),
                    Config.FullInventoryAlert.TextAlert(
                        textAlert.boolean("enabled") ?: return null,
                        textAlert.int("cooldownInSeconds") ?: return null
                    ),
                    Config.FullInventoryAlert.TitleAlert(
                        titleAlert.boolean("enabled") ?: return null,
                        titleAlert.int("cooldownInSeconds") ?: return null
                    )
                ),
                animation = Config.Animation(
                    animation.boolean("enabled") ?: return null,
                    animation["blocksPerSecond"]?.jsonPrimitive?.double ?: return null,
                    animation.boolean("canOtherPlayersPickup") ?: return null
                )
            )
        }
        5 -> Json.decodeFromJsonElement<Config>(jsonElement)
        else -> null
    }
}

private fun legacyConditionStatement(needEnchantment: Boolean, needSneak: Boolean) = ConditionChain(
    when {
        needEnchantment && !needSneak -> "ENCHANTMENT"
        needSneak && !needEnchantment -> "SNEAK"
        else -> "ENCHANTMENT && SNEAK"
    }
)

fun configRequiresEnchantment(text: String?): Boolean = runCatching {
    val root = Json.parseToJsonElement(text ?: return true).jsonObject
    val config = root["config"] as? JsonObject ?: root
    val statement = config["conditionStatement"]?.jsonObject?.get("raw")?.jsonPrimitive?.content
    if (statement != null) return ConditionChain(statement).references(ConditionType.ENCHANTMENT)

    val legacyFlags = listOf("needEnchantment", "enchantmentRequired").mapNotNull { config[it]?.jsonPrimitive?.boolean }
    legacyFlags.isEmpty() || legacyFlags.any { it }
}.getOrDefault(true)
