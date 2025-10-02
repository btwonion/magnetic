@file:Suppress("UNCHECKED_CAST")

package dev.nyon.magnetic

import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.BreakChainedBlocks.breakChainedBlocks
import dev.nyon.magnetic.extensions.BreakChainedBlocks.trailingBlocks
import dev.nyon.magnetic.extensions.failsLongRangeCheck
import dev.nyon.magnetic.extensions.isAllowedToUseMagnetic
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import io.papermc.paper.event.block.PlayerShearBlockEvent
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Statistic
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerHarvestBlockEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object Listeners {

    @Suppress("unused")
    private val magneticListener = listen<DropEvent> {
        if (!player.isAllowedToUseMagnetic()) return@listen

        if (config.itemsAllowed) {
            items.removeIf { item ->
                val copiedStack = item.clone()
                if (player.inventory.addItem(item).isNotEmpty()) {
                    tickInventoryAlert(player)
                    return@removeIf false
                }
                if (copiedStack.amount != 0) player.incrementStatistic(
                    Statistic.PICKUP, copiedStack.type, copiedStack.amount
                )
                true
            }
        }
        if (config.expAllowed) {
            player.giveExp(exp.value, true)
            exp.value = 0
        }
    }

    fun listenForBukkitEvents() {
        listen<BlockDropItemEvent> {
            // Return before calling the DropEvent to prevent executing expensive logic
            if (!player.isAllowedToUseMagnetic()) return@listen
            if (items.isEmpty()) return@listen

            val itemStacks = items.map { it.itemStack }.toMutableList()

            // Check for break-chained block upward and downward
            if (breakChainedBlocks.contains(blockState.type)) handleBreakChainedBlocks(
                block, blockState, player, itemStacks
            )
            else listOf(BlockFace.UP, BlockFace.DOWN).forEach { direction ->
                val other = block.getRelative(direction)
                if (breakChainedBlocks.contains(other.state.type) && other.state.type.breakDirections()
                        .contains(direction)
                ) handleBreakChainedBlocks(
                    other, other.state, player, itemStacks, dontIgnoreRoot = true
                )
            }

            DropEvent(itemStacks, MutableInt(), player).also(Event::callEvent)

            // Delete items that have been added to the inventory
            items.clear()
            itemStacks.forEach { stack ->
                player.world.dropItemNaturally(block.location, stack)
            }
        }

        listen<BlockBreakEvent> {
            val mutableInt = MutableInt(expToDrop)
            DropEvent(mutableListOf(), mutableInt, player).also(Event::callEvent)
            expToDrop = mutableInt.value
        }

        listen<EntityDeathEvent> {
            if (entityType.isIgnored) return@listen
            if (entity.lastDamageCause.failsLongRangeCheck()) return@listen
            val killer = entity.killer ?: return@listen
            val mutableInt = MutableInt(droppedExp)
            val itemStacks = drops.toMutableList()
            DropEvent(itemStacks, mutableInt, killer).also(Event::callEvent)
            droppedExp = mutableInt.value

            // Delete items that have been added to the inventory
            drops.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }

        listen<PlayerShearBlockEvent> {
            val itemStacks = drops.toMutableList()
            DropEvent(itemStacks, MutableInt(), player).also(Event::callEvent)

            // Delete items that have been added to the inventory
            drops.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }

        listen<PlayerShearEntityEvent> {
            val itemStacks = drops.toMutableList()
            DropEvent(itemStacks, MutableInt(), player).also(Event::callEvent)

            // Delete items that have been added to the inventory
            drops.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }

        listen<PlayerFishEvent> {
            val mutableInt = MutableInt(expToDrop)
            DropEvent(mutableListOf(), mutableInt, player).also(Event::callEvent)
            expToDrop = mutableInt.value
        }

        listen<PlayerHarvestBlockEvent> {
            val itemStacks = itemsHarvested.toMutableList()
            DropEvent(itemStacks, MutableInt(), player).also(Event::callEvent)

            // Delete items that have been added to the inventory
            itemsHarvested.removeIf { item ->
                itemStacks.none { stack -> stack.isSimilar(item) }
            }
        }
    }

    fun listenForModEvents() { // Add Veinminer integration
        if (Bukkit.getPluginManager().isPluginEnabled("Veinminer")) {
            listen<de.miraculixx.veinminer.VeinMinerEvent.VeinminerDropEvent> {
                val mutableInt = MutableInt(exp)
                val itemStacks = items.toMutableList()
                DropEvent(itemStacks, mutableInt, player).also(Event::callEvent)
                exp = mutableInt.value

                // Delete items that have been added to the inventory
                items.removeIf { item ->
                    itemStacks.none { stack -> stack.isSimilar(item) }
                }
            }
        }
    }

    private fun handleBreakChainedBlocks(
        block: Block,
        blockState: BlockState,
        player: Player,
        itemStacks: MutableList<ItemStack>,
        dontIgnoreRoot: Boolean = false
    ) {
        val blockFaces = blockState.type.breakDirections()

        val affectedBlocks: MutableSet<Block> = mutableSetOf()
        if (dontIgnoreRoot) affectedBlocks.add(block)
        fun scanSurroundingBlocks(block: Block) {
            blockFaces.forEach { face ->
                val otherBlock = block.getRelative(face)
                if (otherBlock == block) return@forEach
                if (!blockState.type.alsoBreakType(otherBlock.state.type)) return@forEach
                if (affectedBlocks.add(otherBlock) && otherBlock.state.type == blockState.type) scanSurroundingBlocks(
                    otherBlock
                )
            }
        }

        // Recursively scan for all matching blocks that are directly connected to the broken block
        scanSurroundingBlocks(block)

        // Save the drops of the blocks
        affectedBlocks.forEach { affectedBlock ->
            itemStacks.addAll(affectedBlock.getDrops(player.inventory.itemInMainHand, player))
        }

        // Destroy the blocks in reversed order - prevent blocks like Cactus Flowers to break through ticking before us
        affectedBlocks.reversed().forEach { affectedBlock ->
            affectedBlock.type = Material.AIR
        }
    }

    private fun Material.breakDirections(): List<BlockFace> {
        return when (this) {
            Material.BAMBOO, Material.CACTUS, Material.SUGAR_CANE, Material.KELP, Material.KELP_PLANT, Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT -> listOf(
                BlockFace.UP
            )
            Material.CAVE_VINES, Material.CAVE_VINES_PLANT, Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT -> listOf(
                BlockFace.DOWN
            )
            Material.CHORUS_PLANT, Material.CHORUS_FLOWER -> BlockFace.entries.filter(BlockFace::isCartesian)
            Material.SCAFFOLDING -> BlockFace.entries.filter { it.isCartesian && it != BlockFace.DOWN }
            else -> listOf()
        }
    }

    private fun Material.alsoBreakType(other: Material): Boolean {
        if (this == other) return true
        val byProduct = trailingBlocks[this] ?: return false
        return other == byProduct
    }

    private val cooldowns: Map<Config.FullInventoryAlert.Alert, MutableMap<UUID, Instant>> = mapOf(
        config.fullInventoryAlert.soundAlert to mutableMapOf(),
        config.fullInventoryAlert.textAlert to mutableMapOf(),
        config.fullInventoryAlert.titleAlert to mutableMapOf()
    )

    private fun tickInventoryAlert(player: Player) {
        val currentTime = Clock.System.now()
        val uuid = player.playerProfile.id ?: return
        cooldowns.forEach { (alert, playerCooldowns) ->
            if (!alert.enabled) return@forEach
            val lastAlert = playerCooldowns[uuid]
            if (lastAlert == null || currentTime > lastAlert + alert.cooldownInSeconds.seconds) {
                playerCooldowns[uuid] = currentTime
                alert.invoke(player)
            }
        }
    }
}
