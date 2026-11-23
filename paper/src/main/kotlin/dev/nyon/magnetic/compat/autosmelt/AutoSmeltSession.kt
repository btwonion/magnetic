package dev.nyon.magnetic.compat.autosmelt

import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SingleRecipeInput
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Tag
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Applies [AutoSmeltProfiles] to the drops of a single Magnetic pickup.
 *
 * Only collected items stay smelted: anything left in the drop list afterwards is reverted, so the
 * datapack still smelts it as an item entity and nothing is smelted twice.
 */
internal class AutoSmeltSession private constructor(
    private val drops: List<SmeltedDrop<ItemStack>>
) {
    fun isSmelted(stack: ItemStack): Boolean = drops.any { it.result === stack }

    /** Reverts smelted stacks that were not collected and returns the experience earned by the rest. */
    fun finish(items: MutableList<ItemStack>): Int {
        var experience = 0
        drops.forEach { drop ->
            val index = items.indexOfFirst { it === drop.result }
            // Inventory insertion shrinks the stack in place and keeps it in the list when it does not fit.
            val remaining = if (index >= 0) drop.result.amount else 0
            if (index >= 0 && remaining > 0) items[index] = drop.original.clone().apply { amount = remaining }

            val collected = drop.original.amount - remaining
            experience += AutoSmeltProfiles.experienceFor(drop, collected) { Random.nextFloat() }
        }
        return experience
    }

    companion object {
        /** Smelts eligible stacks in [items] in place, or returns null if the tool has no auto-smelt enchantment. */
        fun start(items: MutableList<ItemStack>, player: Player): AutoSmeltSession? {
            if (items.isEmpty()) return null
            val platform = PaperSmeltPlatform(player)
            val profiles = AutoSmeltProfiles.activeProfiles(platform)
            if (profiles.isEmpty()) return null

            val drops = mutableListOf<SmeltedDrop<ItemStack>>()
            items.replaceAll { stack ->
                if (stack.isEmpty) return@replaceAll stack
                val drop = AutoSmeltProfiles.smelt(stack, profiles, platform) ?: return@replaceAll stack
                drops += drop
                drop.result
            }
            return if (drops.isEmpty()) null else AutoSmeltSession(drops)
        }
    }
}

private class PaperSmeltPlatform(private val player: Player) : SmeltPlatform<ItemStack> {
    private val enchantmentIds: Set<String> = player.inventory.itemInMainHand.enchantments.keys
        .mapTo(mutableSetOf()) { it.key.asString() }

    override val isSneaking: Boolean
        get() = player.isSneaking

    override fun toolHasEnchantment(enchantment: String): Boolean = enchantment in enchantmentIds

    override fun itemTagExists(tag: String): Boolean = itemTag(tag)?.values?.isNotEmpty() == true

    override fun isInTag(stack: ItemStack, tag: String): Boolean = itemTag(tag)?.isTagged(stack.type) == true

    override fun itemId(stack: ItemStack): String = stack.type.key.asString()

    override fun furnaceResult(stack: ItemStack): SmeltResult<ItemStack>? {
        val level = (player.world as CraftWorld).handle
        val input = SingleRecipeInput(CraftItemStack.asNMSCopy(stack))
        val recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level).orElse(null)?.value()
            ?: return null
        val result = recipe.assemble(input)
        if (result.isEmpty) return null
        return SmeltResult(
            CraftItemStack.asBukkitCopy(result).apply { amount = stack.amount },
            recipe.experience()
        )
    }

    override fun convert(stack: ItemStack, item: String): ItemStack? {
        val key = NamespacedKey.fromString(item) ?: return null
        val material = Registry.MATERIAL.get(key) ?: return null
        return ItemStack(material, stack.amount)
    }

    private fun itemTag(tag: String): Tag<Material>? {
        val key = NamespacedKey.fromString(tag) ?: return null
        return Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material::class.java)
    }
}
