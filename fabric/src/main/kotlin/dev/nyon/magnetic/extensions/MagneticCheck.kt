package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.ignoredEntities
import dev.nyon.magnetic.config.loadIgnoredEntities
import dev.nyon.magnetic.datagen.magneticEffectId
import dev.nyon.magnetic.datagen.rangedWeaponKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.enchantment.EnchantmentHelper

private const val magneticPermission = "magnetic.ability.use"

fun ServerPlayer.isAllowedToUseMagnetic(): Boolean {
    if (config.enchantmentRequired
        && !EnchantmentHelper.hasTag(mainHandItem, magneticEffectId)
        && !EnchantmentHelper.hasTag(offhandItem, magneticEffectId)
    ) return false
    if (config.sneakRequired && !isCrouching) return false
    if (config.permissionRequired && !me.lucko.fabric.api.permissions.v0.Permissions.check(this, magneticPermission))
        return false

    return true
}

private var ignoredEntitiesInitialized = false
val EntityType<*>.isIgnored: Boolean
    get() {
        if (!ignoredEntitiesInitialized) {
            ignoredEntities = loadIgnoredEntities()
            ignoredEntitiesInitialized = true
        }
        return ignoredEntities.contains(EntityType.getKey(this))
    }

fun DamageSource?.failsLongRangeCheck(): Boolean {
    if (this == null) return true
    return config.ignoreRangedWeapons && weaponItem?.`is`(rangedWeaponKey) == true
}