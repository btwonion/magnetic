package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.datagen.magneticEffectId
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.enchantment.EnchantmentHelper

private const val magneticPermission = "magnetic.ability.use"

fun ServerPlayer.isAllowedToUseMagnetic(): Boolean {
    if (config.needEnchantment
        && !EnchantmentHelper.hasTag(mainHandItem, magneticEffectId)
        && !EnchantmentHelper.hasTag(offhandItem, magneticEffectId)
    ) return false
    if (config.needSneak && !isCrouching) return false
    if (config.needPermission && !me.lucko.fabric.api.permissions.v0.Permissions.check(this, magneticPermission)) return false

    return true
}