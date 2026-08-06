package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.BreakChainedBlocks
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import io.papermc.paper.event.block.PlayerShearBlockEvent
import net.minecraft.world.level.material.WaterFluid
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.player.PlayerHarvestBlockEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import org.bukkit.inventory.ItemStack

@Suppress("unused")
object BlockListeners {

    private val blockDropItemEvent =
        listen<BlockDropItemEvent> { // Return before calling the DropEvent to prevent executing expensive logic
            if (blockState.type.isIgnored) return@listen
            val authorization = DropEventDispatcher.authorize(player) ?: return@listen

            val itemStacks = items.map { it.itemStack }.toMutableList()

            // Check for break-chained block upward and downward
            if (BreakChainedBlocks.breakChainedBlocks.contains(blockState.type)) handleBreakChainedBlocks(
                block, blockState, player, itemStacks
            )
            else listOf(BlockFace.UP, BlockFace.DOWN).forEach { direction ->
                val other = block.getRelative(direction)
                val otherType = other.state.type
                if (!otherType.isIgnored && BreakChainedBlocks.breakChainedBlocks.contains(otherType) && otherType.breakDirections()
                        .contains(direction) && !BreakChainedBlocks.ignoredIndirectChainedBlocks.contains(otherType)
                ) handleBreakChainedBlocks(
                    other, other.state, player, itemStacks, dontIgnoreRoot = true
                )
            }

            DropEventDispatcher.callAuthorized(
                DropEvent(itemStacks, MutableInt(), player, block.location),
                authorization
            )

            // Delete items that have been added to the inventory
            items.clear()
            itemStacks.forEach { stack ->
                player.world.dropItemNaturally(block.location, stack)
            }
        }

    private val harvestBlockEvent = listen<PlayerHarvestBlockEvent> {
        if (harvestedBlock.type.isIgnored) return@listen
        val itemStacks = itemsHarvested.toMutableList()
        DropEventDispatcher.call(DropEvent(itemStacks, MutableInt(), player, harvestedBlock.location))

        // Delete items that have been added to the inventory
        itemsHarvested.removeIf { item ->
            itemStacks.none { stack -> stack.isSimilar(item) }
        }
    }

    private val playerShearBlockEvent = listen<PlayerShearBlockEvent> {
        if (block.type.isIgnored) return@listen
        val itemStacks = drops.toMutableList()
        DropEventDispatcher.call(DropEvent(itemStacks, MutableInt(), player, block.location))

        // Delete items that have been added to the inventory
        drops.removeIf { item ->
            itemStacks.none { stack -> stack.isSimilar(item) }
        }
    }

    private val blockBreakEvent = listen<BlockBreakEvent> {
        if (block.type.isIgnored) return@listen
        val mutableInt = MutableInt(expToDrop)
        DropEventDispatcher.call(DropEvent(mutableListOf(), mutableInt, player, block.location))
        expToDrop = mutableInt.toInt()
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
                if (otherBlock.type.isIgnored) return@forEach
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
            Main.INSTANCE.server.regionScheduler.execute(Main.Companion.INSTANCE, affectedBlock.location) {
                affectedBlock.type =
                    if ((affectedBlock as CraftBlock).blockState.fluidState.type is WaterFluid) Material.WATER else Material.AIR
            }
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
        val byProduct = BreakChainedBlocks.trailingBlocks[this] ?: return false
        return other == byProduct
    }
}
