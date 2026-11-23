package dev.nyon.magnetic.compat.autosmelt

import dev.nyon.magnetic.extensions.magneticIdentifier
import dev.nyon.magnetic.extensions.minecraftIdentifier
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SingleRecipeInput

/**
 * Applies [AutoSmeltProfiles] to the drops of a single Magnetic pickup.
 *
 * Only collected items stay smelted: anything left in the drop list afterwards is reverted, so the
 * datapack still smelts it as an item entity and nothing is smelted twice.
 */
internal class AutoSmeltSession private constructor(
    private val player: ServerPlayer,
    private val drops: List<SmeltedDrop<ItemStack>>
) {
    fun isSmelted(stack: ItemStack): Boolean = drops.any { it.result === stack }

    /** Reverts smelted stacks that were not collected and returns the experience earned by the rest. */
    fun finish(items: MutableList<ItemStack>): Int {
        var experience = 0
        drops.forEach { drop ->
            val index = items.indexOfFirst { it === drop.result }
            val remaining = if (index >= 0) drop.result.count else 0
            if (index >= 0 && remaining > 0) items[index] = drop.original.copyWithCount(remaining)

            val collected = drop.original.count - remaining
            experience += AutoSmeltProfiles.experienceFor(drop, collected) { player.random.nextFloat() }
        }
        return experience
    }

    companion object {
        /** Smelts eligible stacks in [items] in place, or returns null if the tool has no auto-smelt enchantment. */
        fun start(items: MutableList<ItemStack>, player: ServerPlayer): AutoSmeltSession? {
            if (items.isEmpty()) return null
            val platform = MinecraftSmeltPlatform(player)
            val profiles = AutoSmeltProfiles.activeProfiles(platform)
            if (profiles.isEmpty()) return null

            val drops = mutableListOf<SmeltedDrop<ItemStack>>()
            items.replaceAll { stack ->
                if (stack.isEmpty) return@replaceAll stack
                val drop = AutoSmeltProfiles.smelt(stack, profiles, platform) ?: return@replaceAll stack
                drops += drop
                drop.result
            }
            return if (drops.isEmpty()) null else AutoSmeltSession(player, drops)
        }
    }
}

private class MinecraftSmeltPlatform(private val player: ServerPlayer) : SmeltPlatform<ItemStack> {
    private val level: ServerLevel =
        /*? if >=1.21.11 {*/ player.level() /*?} else {*/ /*player.serverLevel() *//*?}*/
    private val enchantmentIds: Set<String> = player.mainHandItem.get(DataComponents.ENCHANTMENTS)
        ?.keySet()
        ?.mapNotNull { holder -> holder.unwrapKey().orElse(null)?.magneticIdentifier()?.toString() }
        ?.toSet()
        .orEmpty()

    override val isSneaking: Boolean
        get() = player.isShiftKeyDown

    override fun toolHasEnchantment(enchantment: String): Boolean = enchantment in enchantmentIds

    override fun itemTagExists(tag: String): Boolean {
        return BuiltInRegistries.ITEM.getTagOrEmpty(itemTag(tag)).iterator().hasNext()
    }

    override fun isInTag(stack: ItemStack, tag: String): Boolean = stack.`is`(itemTag(tag))

    override fun itemId(stack: ItemStack): String = BuiltInRegistries.ITEM.getKey(stack.item).toString()

    override fun furnaceResult(stack: ItemStack): SmeltResult<ItemStack>? {
        val input = SingleRecipeInput(stack)
        /*? if >=26.1 {*/
        val recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level).orElse(null)?.value()
            ?: return null
        val result = recipe.assemble(input)
        val experience = recipe.experience()
        /*?} else if >=1.21.11 {*/
        /*val recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level).orElse(null)?.value()
            ?: return null
        val result = recipe.assemble(input, level.registryAccess())
        val experience = recipe.experience()
        *//*?} else {*/
        /*val recipe = level.recipeManager.getRecipeFor(RecipeType.SMELTING, input, level).orElse(null)?.value()
            ?: return null
        val result = recipe.assemble(input, level.registryAccess())
        val experience = recipe.experience
        *//*?}*/
        if (result.isEmpty) return null
        return SmeltResult(result.copyWithCount(stack.count), experience)
    }

    override fun convert(stack: ItemStack, item: String): ItemStack? {
        val (namespace, path) = item.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
        val target = BuiltInRegistries.ITEM.getOptional(minecraftIdentifier(namespace, path)).orElse(null)
            ?: return null
        return ItemStack(target, stack.count)
    }

    private fun itemTag(tag: String): TagKey<net.minecraft.world.item.Item> {
        val (namespace, path) = tag.split(':', limit = 2)
        return TagKey.create(Registries.ITEM, minecraftIdentifier(namespace, path))
    }
}
