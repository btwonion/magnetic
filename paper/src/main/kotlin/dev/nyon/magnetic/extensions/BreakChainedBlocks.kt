package dev.nyon.magnetic.extensions

import org.bukkit.Material

object BreakChainedBlocks {
    val endGrowingPlants = setOf(
        Material.CHORUS_FLOWER, Material.CHORUS_PLANT
    )
    val netherGrowingPlants = setOf(
        Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT, Material.WEEPING_VINES_PLANT, Material.WEEPING_VINES
    )
    val overworldGrowingPlants = setOf(
        Material.BAMBOO,
        Material.CACTUS,
        Material.KELP,
        Material.KELP_PLANT,
        Material.SUGAR_CANE,
        Material.CAVE_VINES,
        Material.CAVE_VINES_PLANT
    )

    val breakChainedBlocks = setOf(
        endGrowingPlants, netherGrowingPlants, overworldGrowingPlants
    ).flatten()

    val trailingBlocks = mapOf(
        Material.CACTUS to Material.CACTUS_FLOWER,
        Material.KELP_PLANT to Material.KELP,
        Material.TWISTING_VINES_PLANT to Material.TWISTING_VINES,
        Material.WEEPING_VINES_PLANT to Material.WEEPING_VINES,
        Material.CAVE_VINES_PLANT to Material.CAVE_VINES
    )
}