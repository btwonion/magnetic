package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.mixins.ExperienceOrbInvoker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.apache.commons.lang3.mutable.MutableInt
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

object DropEvent {
    private val animationScope = CoroutineScope(Dispatchers.Default)
    private val animationDelay = 50.milliseconds
    private val ticksPerAnimation = config.animationSpeedInMs / animationDelay.inWholeMilliseconds.toInt()

    val event: Event<DropEventConsumer> = EventFactory.createArrayBacked(DropEventConsumer::class.java) { listeners ->
        DropEventConsumer { items, exp, player, pos ->
            listeners.forEach {
                it(items, exp, player, pos)
            }
        }
    }

    @Suppress("unused")
    private val listener = event.register { items, exp, player, pos ->
        if (config.needSneak && !player.isCrouching) return@register
        if (config.needEnchantment && !EnchantmentHelper.hasTag(
                player.mainHandItem,
                magneticEffectId
            ) && !EnchantmentHelper.hasTag(player.offhandItem, magneticEffectId)
        ) return@register

        if (config.expAllowed) addExp(exp, player)
        if (!config.itemsAllowed || items.isEmpty()) return@register

        if (config.animation) pullAndAddItems(items, player, pos)
        else items.removeIf(player::addItem)
    }

    private fun pullAndAddItems(items: MutableList<ItemStack>, player: ServerPlayer, pos: BlockPos) {
        val itemCopies = items.toMutableList()
        items.clear()
        animationScope.launch {
            (0 .. ticksPerAnimation).forEach { index ->
                val animationProgress = index / ticksPerAnimation.toDouble()
                val playerPosition = player.position()
                // Animate ItemEntity
                val currentPos = pos.center.lerp(playerPosition, animationProgress)

                // Animate particles
                val particlePositions = listOf(0.05, 0.1, 0.15).map { pos.center.lerp(playerPosition, max(animationProgress + it, 1.0)) }
                particlePositions.forEach {
                    player.level().addParticle(
                        ParticleTypes.EXPLOSION,
                        false,
                        true,
                        it.x,
                        it.y,
                        it.z,
                        0.0,
                        0.0,
                        0.0
                    )
                }

                delay(animationDelay)
            }

            // Add items to the player or drop at location
            itemCopies.forEach {
                if (!player.addItem(it)) {
                    val level = player.level()
                    level.addFreshEntity(ItemEntity(level, player.x, player.y, player.z, it))
                }
            }
        }
    }

    private fun addExp(exp: MutableInt, player: ServerPlayer) {
        val fakeExperienceOrb = ExperienceOrb(player.level(), 0.0, 0.0, 0.0, exp.value)
        player.take(fakeExperienceOrb, 1)
        val leftExp = (fakeExperienceOrb as ExperienceOrbInvoker).invokeRepairPlayerItems(player, exp.value)
        if (leftExp > 0) player.giveExperiencePoints(leftExp)
        exp.value = 0
    }
}

fun interface DropEventConsumer {
    operator fun invoke(items: MutableList<ItemStack>, exp: MutableInt, player: ServerPlayer, pos: BlockPos)
}