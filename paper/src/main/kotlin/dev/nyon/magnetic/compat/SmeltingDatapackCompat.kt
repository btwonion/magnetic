package dev.nyon.magnetic.compat

import dev.nyon.magnetic.DropAuthorization
import dev.nyon.magnetic.DropEntityDispatcher
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.listen
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Compatibility with auto-smelt datapacks that smelt dropped *item entities* a few ticks after a block was mined.
 *
 * Magnetic normally collects drops before an item entity exists, so these datapacks never see them. While the
 * mining tool carries one of their enchantments, block drops spawn normally and are collected once the datapack
 * has had time to smelt them.
 */
internal object SmeltingDatapackCompat {
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
    private const val COLLECTION_DELAY_TICKS = 8L
    private val deferredDropKey = NamespacedKey("magnetic", "smelting_datapack_deferred")

    private class PendingVeinminerDrops(
        val authorization: DropAuthorization,
        val tick: Int,
        val stacks: MutableList<ItemStack>
    )

    // Veinminer spawns its drops right after its drop event returns, on the same thread and tick.
    private val pendingVeinminerDrops = ThreadLocal<PendingVeinminerDrops>()

    fun listenForEvents() {
        listen<ItemSpawnEvent>(EventPriority.MONITOR) {
            if (isCancelled) return@listen
            val pending = pendingVeinminerDrops.get() ?: return@listen
            if (pending.tick != Bukkit.getCurrentTick()) {
                pendingVeinminerDrops.remove()
                return@listen
            }

            val index = pending.stacks.indexOfFirst { it == entity.itemStack }
            if (index < 0) return@listen
            pending.stacks.removeAt(index)
            if (pending.stacks.isEmpty()) pendingVeinminerDrops.remove()
            collectAfterDatapack(entity, pending.authorization)
        }
    }

    /** Whether [player]'s drops must spawn first because the tool carries an item-entity auto-smelt enchantment. */
    fun defersDrops(player: Player): Boolean {
        if (!config.itemsAllowed) return false
        return player.inventory.itemInMainHand.enchantments.keys.any { it.key.asString() in itemEntityEnchantments }
    }

    fun isDeferred(item: Item): Boolean = item.persistentDataContainer.has(deferredDropKey)

    /** Collects [item] for the authorized player once the datapack had time to smelt it. */
    fun collectAfterDatapack(item: Item, authorization: DropAuthorization) {
        item.persistentDataContainer.set(deferredDropKey, PersistentDataType.BYTE, 1.toByte())
        item.scheduler.execute(Main.INSTANCE, {
            if (!item.isValid) return@execute
            DropEntityDispatcher.claim(item) { claimedDrop ->
                DropEntityDispatcher.dispatch(claimedDrop, authorization, deferredDropKey)
            }
        }, null, COLLECTION_DELAY_TICKS)
    }

    /** Defers the drops Veinminer is about to spawn for the authorized player. */
    fun deferVeinminerDrops(items: List<ItemStack>, authorization: DropAuthorization) {
        if (items.isEmpty()) return
        pendingVeinminerDrops.set(
            PendingVeinminerDrops(authorization, Bukkit.getCurrentTick(), items.mapTo(mutableListOf(), ItemStack::clone))
        )
    }
}
