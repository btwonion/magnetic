package dev.nyon.magnetic.compat.autosmelt

/**
 * Auto-smelt datapacks that smelt *item entities* after a block was mined.
 *
 * Magnetic moves drops into the inventory before an item entity exists, so those datapacks never see the
 * drops. When the mining tool carries one of their enchantments, Magnetic applies the datapack's smelting
 * itself. Auto-smelt projects that change loot tables or block drops before items spawn need no profile.
 *
 * This file is free of Minecraft types. It mirrors the mod's copy, which is covered by unit tests; keep both in sync.
 */
internal object AutoSmeltProfiles {
    private const val SMELTING_ENCHANTMENT_TAG = "smelting_enchantment:smelting"

    /** Profiles are checked in order; the first active profile that accepts a drop smelts it. */
    val profiles: List<AutoSmeltProfile> = listOf(
        // https://modrinth.com/datapack/smelting-enchantment
        AutoSmeltProfile(
            name = "Smelting Enchantment",
            enchantment = "smelting_enchantment:smelting",
            inputs = SmeltInputs.Tag(SMELTING_ENCHANTMENT_TAG),
            experience = SmeltExperience.ONE_PER_STACK,
            requiredItemTag = SMELTING_ENCHANTMENT_TAG
        ),
        // https://modrinth.com/datapack/veinmine-autosmelt-enchantments
        // Reuses the enchantment ID above but has no item tag and maps raw blocks itself.
        AutoSmeltProfile(
            name = "Veinmine & Autosmelt Enchantments",
            enchantment = "smelting_enchantment:smelting",
            inputs = SmeltInputs.Items(
                mapOf(
                    "minecraft:raw_copper" to SmeltOutput.Fixed("minecraft:copper_ingot"),
                    "minecraft:raw_copper_block" to SmeltOutput.Fixed("minecraft:copper_block"),
                    "minecraft:raw_gold" to SmeltOutput.Fixed("minecraft:gold_ingot"),
                    "minecraft:raw_gold_block" to SmeltOutput.Fixed("minecraft:gold_block"),
                    "minecraft:raw_iron" to SmeltOutput.Fixed("minecraft:iron_ingot"),
                    "minecraft:raw_iron_block" to SmeltOutput.Fixed("minecraft:iron_block")
                )
            ),
            experience = SmeltExperience.NONE,
            forbiddenItemTag = SMELTING_ENCHANTMENT_TAG
        ),
        // https://modrinth.com/datapack/smelt-mining
        AutoSmeltProfile(
            name = "Smelt Mining",
            enchantment = "smelt_mining:smelt_mining",
            inputs = SmeltInputs.furnace(
                "minecraft:raw_iron",
                "minecraft:raw_gold",
                "minecraft:raw_copper",
                "minecraft:ancient_debris"
            ),
            experience = SmeltExperience.RECIPE,
            skipWhileSneaking = true
        ),
        // https://modrinth.com/datapack/smelt-enchant
        // Mirrors the pack's item predicate, which misspells the crimson entries and therefore skips them.
        AutoSmeltProfile(
            name = "Smelt Enchant",
            enchantment = "smelt:smelt",
            inputs = SmeltInputs.furnace(
                "minecraft:raw_iron", "minecraft:raw_copper", "minecraft:raw_gold", "minecraft:ancient_debris",
                "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore", "minecraft:coal_ore",
                "minecraft:deepslate_coal_ore", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", "minecraft:diamond_ore",
                "minecraft:deepslate_diamond_ore", "minecraft:nether_quartz_ore", "minecraft:nether_gold_ore",
                "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:copper_ore",
                "minecraft:deepslate_copper_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                "minecraft:cobblestone", "minecraft:stone", "minecraft:stone_bricks", "minecraft:cobbled_deepslate",
                "minecraft:deepslate_bricks", "minecraft:deepslate_tiles", "minecraft:sandstone",
                "minecraft:red_sandstone", "minecraft:nether_bricks", "minecraft:basalt",
                "minecraft:polished_blackstone_bricks", "minecraft:quartz_block", "minecraft:clay", "minecraft:sand",
                "minecraft:red_sand", "minecraft:wet_sponge", "minecraft:chorus_fruit", "minecraft:sea_pickle",
                "minecraft:cactus", "minecraft:clay_ball", "minecraft:netherrack", "minecraft:resin_clump",
                "minecraft:red_terracotta", "minecraft:blue_terracotta", "minecraft:cyan_terracotta",
                "minecraft:gray_terracotta", "minecraft:lime_terracotta", "minecraft:pink_terracotta",
                "minecraft:black_terracotta", "minecraft:brown_terracotta", "minecraft:green_terracotta",
                "minecraft:white_terracotta", "minecraft:orange_terracotta", "minecraft:purple_terracotta",
                "minecraft:yellow_terracotta", "minecraft:magenta_terracotta", "minecraft:light_blue_terracotta",
                "minecraft:light_gray_terracotta", "minecraft:oak_log", "minecraft:spruce_log",
                "minecraft:birch_log", "minecraft:jungle_log", "minecraft:acacia_log", "minecraft:dark_oak_log",
                "minecraft:mangrove_log", "minecraft:cherry_log", "minecraft:pale_oak_log",
                "minecraft:warped_stem", "minecraft:oak_wood", "minecraft:spruce_wood",
                "minecraft:birch_wood", "minecraft:jungle_wood", "minecraft:acacia_wood",
                "minecraft:dark_oak_wood", "minecraft:mangrove_wood", "minecraft:cherry_wood",
                "minecraft:pale_oak_wood", "minecraft:warped_hyphae",
                "minecraft:stripped_oak_log", "minecraft:stripped_spruce_log", "minecraft:stripped_birch_log",
                "minecraft:stripped_jungle_log", "minecraft:stripped_acacia_log",
                "minecraft:stripped_dark_oak_log", "minecraft:stripped_mangrove_log",
                "minecraft:stripped_cherry_log", "minecraft:stripped_pale_oak_log",
                "minecraft:stripped_warped_stem",
                "minecraft:stripped_oak_wood", "minecraft:stripped_spruce_wood", "minecraft:stripped_birch_wood",
                "minecraft:stripped_jungle_wood", "minecraft:stripped_acacia_wood",
                "minecraft:stripped_dark_oak_wood", "minecraft:stripped_mangrove_wood",
                "minecraft:stripped_cherry_wood", "minecraft:stripped_pale_oak_wood",
                "minecraft:stripped_warped_hyphae", "minecraft:oak_leaves",
                "minecraft:spruce_leaves", "minecraft:birch_leaves", "minecraft:jungle_leaves",
                "minecraft:acacia_leaves", "minecraft:dark_oak_leaves", "minecraft:mangrove_leaves",
                "minecraft:cherry_leaves", "minecraft:pale_oak_leaves", "minecraft:azalea_leaves",
                "minecraft:flowering_azalea_leaves"
            ),
            experience = SmeltExperience.NONE
        )
    )

    /** Returns the profiles whose enchantment is on the tool and whose other preconditions hold. */
    fun <S> activeProfiles(platform: SmeltPlatform<S>): List<AutoSmeltProfile> {
        return profiles.filter { profile ->
            platform.toolHasEnchantment(profile.enchantment) &&
                !(profile.skipWhileSneaking && platform.isSneaking) &&
                (profile.requiredItemTag == null || platform.itemTagExists(profile.requiredItemTag)) &&
                (profile.forbiddenItemTag == null || !platform.itemTagExists(profile.forbiddenItemTag))
        }
    }

    /** Smelts [stack] with the first of [profiles] that accepts it, or returns null if none does. */
    fun <S> smelt(stack: S, profiles: List<AutoSmeltProfile>, platform: SmeltPlatform<S>): SmeltedDrop<S>? {
        for (profile in profiles) {
            val output = profile.inputs.outputFor(stack, platform) ?: continue
            val smelted = when (output) {
                is SmeltOutput.Fixed -> platform.convert(stack, output.item)?.let { SmeltResult(it, 0f) }
                SmeltOutput.Furnace -> platform.furnaceResult(stack)
            } ?: continue
            return SmeltedDrop(stack, smelted.stack, profile.experience, smelted.experiencePerItem)
        }
        return null
    }

    /**
     * Experience for [collected] items of a smelted drop. Recipe experience is rounded like a furnace:
     * the fractional part is granted with matching probability.
     */
    fun experienceFor(drop: SmeltedDrop<*>, collected: Int, random: () -> Float): Int {
        if (collected <= 0) return 0
        return when (drop.experience) {
            SmeltExperience.NONE -> 0
            SmeltExperience.ONE_PER_STACK -> 1
            SmeltExperience.RECIPE -> {
                val total = collected * drop.recipeExperiencePerItem
                val whole = total.toInt()
                val fraction = total - whole
                if (fraction > 0f && random() < fraction) whole + 1 else whole
            }
        }
    }

    private fun <S> SmeltInputs.outputFor(stack: S, platform: SmeltPlatform<S>): SmeltOutput? {
        return when (this) {
            is SmeltInputs.Tag -> if (platform.isInTag(stack, tag)) SmeltOutput.Furnace else null
            is SmeltInputs.Items -> outputs[platform.itemId(stack)]
        }
    }
}

internal data class AutoSmeltProfile(
    val name: String,
    val enchantment: String,
    val inputs: SmeltInputs,
    val experience: SmeltExperience,
    val skipWhileSneaking: Boolean = false,
    /** The profile only applies when this item tag exists, which identifies the datapack. */
    val requiredItemTag: String? = null,
    /** The profile only applies when this item tag does not exist. */
    val forbiddenItemTag: String? = null
)

internal sealed interface SmeltInputs {
    data class Tag(val tag: String) : SmeltInputs
    data class Items(val outputs: Map<String, SmeltOutput>) : SmeltInputs

    companion object {
        fun furnace(vararg items: String) = Items(items.associateWith { SmeltOutput.Furnace })
    }
}

internal sealed interface SmeltOutput {
    /** Uses the result of the item's furnace recipe, like the `minecraft:furnace_smelt` loot function. */
    data object Furnace : SmeltOutput

    /** Replaces the item with [item], keeping the count. */
    data class Fixed(val item: String) : SmeltOutput
}

internal enum class SmeltExperience {
    NONE,

    /** One experience point per smelted stack, like an orb summoned per item entity. */
    ONE_PER_STACK,

    /** The furnace recipe's experience per smelted item. */
    RECIPE
}

internal data class SmeltResult<S>(val stack: S, val experiencePerItem: Float)

internal data class SmeltedDrop<S>(
    val original: S,
    val result: S,
    val experience: SmeltExperience,
    val recipeExperiencePerItem: Float
)

/** Access to the game state needed to evaluate [AutoSmeltProfiles]. */
internal interface SmeltPlatform<S> {
    val isSneaking: Boolean

    fun toolHasEnchantment(enchantment: String): Boolean
    fun itemTagExists(tag: String): Boolean
    fun isInTag(stack: S, tag: String): Boolean
    fun itemId(stack: S): String

    /** The furnace result with the input's count, or null if [stack] has no smelting recipe. */
    fun furnaceResult(stack: S): SmeltResult<S>?

    /** [stack] converted to [item] with the same count, or null if [item] is unknown. */
    fun convert(stack: S, item: String): S?
}
