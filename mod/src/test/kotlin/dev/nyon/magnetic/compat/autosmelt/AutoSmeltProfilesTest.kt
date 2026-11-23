package dev.nyon.magnetic.compat.autosmelt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AutoSmeltProfilesTest {
    private data class Stack(val item: String, val count: Int)

    private class FakePlatform(
        private val enchantments: Set<String>,
        private val itemTags: Map<String, Set<String>> = emptyMap(),
        override val isSneaking: Boolean = false
    ) : SmeltPlatform<Stack> {
        private val furnace = mapOf(
            "minecraft:raw_iron" to ("minecraft:iron_ingot" to 0.7f),
            "minecraft:raw_gold" to ("minecraft:gold_ingot" to 1.0f),
            "minecraft:ancient_debris" to ("minecraft:netherite_scrap" to 2.0f),
            "minecraft:cobblestone" to ("minecraft:stone" to 0.1f)
        )

        override fun toolHasEnchantment(enchantment: String) = enchantment in enchantments
        override fun itemTagExists(tag: String) = itemTags[tag].orEmpty().isNotEmpty()
        override fun isInTag(stack: Stack, tag: String) = stack.item in itemTags[tag].orEmpty()
        override fun itemId(stack: Stack) = stack.item
        override fun furnaceResult(stack: Stack): SmeltResult<Stack>? {
            val (result, experience) = furnace[stack.item] ?: return null
            return SmeltResult(Stack(result, stack.count), experience)
        }

        override fun convert(stack: Stack, item: String) = Stack(item, stack.count)
    }

    private fun smelt(platform: FakePlatform, stack: Stack): SmeltedDrop<Stack>? {
        return AutoSmeltProfiles.smelt(stack, AutoSmeltProfiles.activeProfiles(platform), platform)
    }

    @Test
    fun `nothing is smelted without an auto-smelt enchantment`() {
        val platform = FakePlatform(enchantments = setOf("minecraft:fortune"))

        assertEquals(emptyList<AutoSmeltProfile>(), AutoSmeltProfiles.activeProfiles(platform))
        assertNull(smelt(platform, Stack("minecraft:raw_iron", 2)))
    }

    @Test
    fun `smelting enchantment smelts its tagged items with one experience per stack`() {
        val platform = FakePlatform(
            enchantments = setOf("smelting_enchantment:smelting"),
            itemTags = mapOf("smelting_enchantment:smelting" to setOf("minecraft:raw_iron"))
        )

        val drop = smelt(platform, Stack("minecraft:raw_iron", 3))!!
        assertEquals(Stack("minecraft:iron_ingot", 3), drop.result)
        assertEquals(1, AutoSmeltProfiles.experienceFor(drop, collected = 3) { 0f })
        // Raw blocks are not in the pack's tag, so the Veinmine & Autosmelt mapping must not apply.
        assertNull(smelt(platform, Stack("minecraft:raw_iron_block", 1)))
    }

    @Test
    fun `veinmine autosmelt pack shares the enchantment but maps raw blocks without experience`() {
        val platform = FakePlatform(enchantments = setOf("smelting_enchantment:smelting"))

        val drop = smelt(platform, Stack("minecraft:raw_iron_block", 2))!!
        assertEquals(Stack("minecraft:iron_block", 2), drop.result)
        assertEquals(0, AutoSmeltProfiles.experienceFor(drop, collected = 2) { 0f })
        assertNull(smelt(platform, Stack("minecraft:cobblestone", 1)))
    }

    @Test
    fun `smelt mining is disabled while sneaking`() {
        val enchantments = setOf("smelt_mining:smelt_mining")

        assertEquals(
            Stack("minecraft:netherite_scrap", 1),
            smelt(FakePlatform(enchantments), Stack("minecraft:ancient_debris", 1))?.result
        )
        assertNull(smelt(FakePlatform(enchantments, isSneaking = true), Stack("minecraft:ancient_debris", 1)))
    }

    @Test
    fun `recipe experience rounds the fractional part by chance`() {
        val drop = smelt(FakePlatform(setOf("smelt_mining:smelt_mining")), Stack("minecraft:raw_iron", 3))!!

        // 3 * 0.7 = 2.1 experience
        assertEquals(3, AutoSmeltProfiles.experienceFor(drop, collected = 3) { 0.05f })
        assertEquals(2, AutoSmeltProfiles.experienceFor(drop, collected = 3) { 0.5f })
        assertEquals(0, AutoSmeltProfiles.experienceFor(drop, collected = 0) { 0f })
    }

    @Test
    fun `items without a furnace recipe stay unchanged`() {
        val platform = FakePlatform(enchantments = setOf("smelt:smelt"))

        assertEquals(Stack("minecraft:stone", 4), smelt(platform, Stack("minecraft:cobblestone", 4))?.result)
        // Listed by the pack, but this fake has no recipe for it.
        assertNull(smelt(platform, Stack("minecraft:sand", 4)))
        // Not listed by the pack.
        assertNull(smelt(platform, Stack("minecraft:dirt", 4)))
    }
}
