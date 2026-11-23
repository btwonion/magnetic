package dev.nyon.magnetic.compat

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.magneticIdentifier
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import org.apache.commons.lang3.mutable.MutableInt
import java.util.concurrent.ConcurrentHashMap

/**
 * Compatibility with auto-smelt datapacks that smelt dropped *item entities* a few ticks after a block was mined.
 *
 * Magnetic normally collects drops before an item entity exists, so these datapacks never see them. While the
 * mining tool carries one of their enchantments, block drops spawn normally and are collected once the datapack
 * has had time to smelt them.
 */
object SmeltingDatapackCompat {
    /** Enchantments of auto-smelt datapacks that act on item entities. */
    private val itemEntityEnchantments = setOf(
        // Smelting Enchantment and Veinmine & Autosmelt Enchantments
        "smelting_enchantment:smelting",
        // Smelt Mining
        "smelt_mining:smelt_mining",
        // Smelt Enchant
        "smelt:smelt"
    )

    /**
     * The slowest supported pack smelts five ticks after the break. Block drops can't be picked up for ten ticks,
     * so the player cannot grab them before Magnetic does.
     */
    const val COLLECTION_DELAY_TICKS = 8L

    private data class PendingDrop(val player: ServerPlayer, val collectAt: Long)

    private val pendingDrops = ConcurrentHashMap<ItemEntity, PendingDrop>()

    /**
     * Lets [itemEntity] spawn and schedules its collection when [player]'s tool has an item-entity auto-smelt
     * enchantment. Returns whether the drop was deferred.
     */
    @JvmStatic
    fun deferIfNeeded(itemEntity: ItemEntity, player: ServerPlayer, level: ServerLevel): Boolean {
        if (!config.itemsAllowed || !hasItemEntityEnchantment(player)) return false
        pendingDrops[itemEntity] = PendingDrop(player, level.gameTime + COLLECTION_DELAY_TICKS)
        return true
    }

    /** Collects the deferred drops of [level] that are due at [gameTime]. */
    @JvmStatic
    fun collectDue(level: ServerLevel, gameTime: Long) {
        pendingDrops.forEach { (itemEntity, pending) ->
            if (itemEntity.level() !== level || gameTime < pending.collectAt) return@forEach
            pendingDrops.remove(itemEntity, pending)
            if (!itemEntity.isAlive || itemEntity.item.isEmpty) return@forEach

            // A respawned or disconnected player is a removed entity, so the drop simply stays on the ground.
            val player = pending.player
            if (player.isRemoved || player.level() !== level) return@forEach

            val items = mutableListOf(itemEntity.item.copy())
            DropEvent(items, MutableInt(0), player, itemEntity.blockPosition())
            val remaining = items.firstOrNull()
            if (remaining == null || remaining.isEmpty) itemEntity.discard() else itemEntity.setItem(remaining)
        }
    }

    private fun hasItemEntityEnchantment(player: ServerPlayer): Boolean {
        val enchantments = player.mainHandItem.get(DataComponents.ENCHANTMENTS) ?: return false
        return enchantments.keySet().any { holder ->
            val id = holder.unwrapKey().orElse(null)?.magneticIdentifier()?.toString()
            id in itemEntityEnchantments
        }
    }
}
