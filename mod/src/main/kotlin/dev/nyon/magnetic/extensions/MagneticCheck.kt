package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.ignoredEntities
import dev.nyon.magnetic.config.reloadIgnoredEntities
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

private var ignoredEntitiesInitialized = false
val EntityType<*>.isIgnored: Boolean
    get() {
        if (!ignoredEntitiesInitialized) {
            reloadIgnoredEntities()
            ignoredEntitiesInitialized = true
        }
        return ignoredEntities.contains(EntityType.getKey(this))
    }

fun Entity.failsLongRangeCheck(player: ServerPlayer): Boolean {
    if (config.ignoredEntitiesRangeMin == -1.0) return false
    return !position().closerThan(player.position(), config.ignoredEntitiesRangeMin)
}