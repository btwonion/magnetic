package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.ignoredBlocks
import dev.nyon.magnetic.config.ignoredEntities
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.state.BlockState

val EntityType<*>.isIgnored: Boolean
    get() = EntityType.getKey(this) in ignoredEntities

val BlockState.isIgnored: Boolean
    get() = BuiltInRegistries.BLOCK.getKey(block) in ignoredBlocks

fun Entity.failsLongRangeCheck(player: ServerPlayer): Boolean {
    if (config.ignoredEntitiesRangeMin == -1.0) return false
    return !position().closerThan(player.position(), config.ignoredEntitiesRangeMin)
}
