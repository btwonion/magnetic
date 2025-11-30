package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.ignoredEntities
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

val EntityType.isIgnored: Boolean
    get() {
        return ignoredEntities.contains(key)
    }

fun Entity.failsLongRangeCheck(player: Player): Boolean {
    if (config.ignoredEntitiesRangeMin == -1.0) return false
    return location.distance(player.location) > config.ignoredEntitiesRangeMin
}