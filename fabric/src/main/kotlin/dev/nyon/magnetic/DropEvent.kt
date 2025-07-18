package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.mixins.ExperienceOrbInvoker
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.apache.commons.lang3.mutable.MutableInt

object DropEvent {
    val event: Event<DropEventConsumer> = EventFactory.createArrayBacked(DropEventConsumer::class.java) { listeners ->
        DropEventConsumer { items, exp, player ->
            listeners.forEach {
                it(items, exp, player)
            }
        }
    }

    @Suppress("unused", "KotlinConstantConditions")
    private val listener = event.register { items, exp, player ->
        if (config.needSneak && !player.isCrouching) return@register
        if (config.needEnchantment && !EnchantmentHelper.hasTag(
                player.mainHandItem, magneticEffectId
            ) && !EnchantmentHelper.hasTag(player.offhandItem, magneticEffectId)
        ) return@register

        if (config.itemsAllowed) {
            items.removeIf { item ->
                val copiedStack = item.copy()
                if (!player.addItem(item)) return@removeIf false
                player.awardStat(Stats.ITEM_PICKED_UP.get(copiedStack.item), copiedStack.count)
                true
            }
        }
        if (config.expAllowed) {
            val fakeExperienceOrb = ExperienceOrb(player.level(), 0.0, 0.0, 0.0, exp.value)
            player.take(fakeExperienceOrb, 1)
            val leftExp = (fakeExperienceOrb as ExperienceOrbInvoker).invokeRepairPlayerItems(player, exp.value)
            if (leftExp > 0) player.giveExperiencePoints(leftExp)
            exp.value = 0
        }
    }
}

fun interface DropEventConsumer {
    operator fun invoke(items: MutableList<ItemStack>, exp: MutableInt, player: ServerPlayer)
}