package dev.nyon.telekinesis

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.enchantment.Enchantment

/*? if <1.21 {*/
/*import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.entity.EquipmentSlot
*//*?}*/
/*? >1.20.5 {*/

/*?} else {*//*
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.MobType
import net.minecraft.world.item.enchantment.EnchantmentCategory
*//*?}*/



/*? if =1.20.6 {*/
/*class TelekinesisEnchantment : Enchantment(
    definition(
        ItemTags.DURABILITY_ENCHANTABLE,
        2,
        1,
        dynamicCost(25, 25),
        dynamicCost(75, 25),
        5,
        *EquipmentSlot.entries.toTypedArray()
    )
) {
    override fun getFullname(i: Int): Component =
        Component.translatable("enchantment.telekinesis.telekinesis.name").withStyle(Style.EMPTY.withColor(0xFFB64C))
}
*//*?} elif <1.20.5 {*//*
class TelekinesisEnchantment : Enchantment(
    Rarity.RARE, EnchantmentCategory.BREAKABLE, listOf(EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND).toTypedArray()
) {

    override fun getMinLevel(): Int = 1
    override fun getMaxLevel(): Int = 1

    override fun getMinCost(i: Int): Int = 25
    override fun getMaxCost(i: Int): Int = 50

    override fun getDamageProtection(i: Int, damageSource: DamageSource): Int = 0
    override fun getDamageBonus(i: Int, mobType: MobType): Float = 0F

    override fun getFullname(i: Int): Component =
        Component.translatable("enchantment.telekinesis.telekinesis.name").withStyle(Style.EMPTY.withColor(0xFFB64C))
}
*//*?}*/

val telekinesisEffectId: TagKey<Enchantment> = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("telekinesis", "auto_move"))
val telekinesisEnchantmentId: ResourceLocation = ResourceLocation.fromNamespaceAndPath("telekinesis", "telekinesis")