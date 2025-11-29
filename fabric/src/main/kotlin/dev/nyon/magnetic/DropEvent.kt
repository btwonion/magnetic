package dev.nyon.magnetic

import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.isAllowedToUseMagnetic
import dev.nyon.magnetic.mixins.ExperienceOrbInvoker
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.mutable.MutableInt
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object DropEvent {
    val event: Event<DropEventConsumer> = EventFactory.createArrayBacked(DropEventConsumer::class.java) { listeners ->
        DropEventConsumer { items, exp, player, pos ->
            listeners.forEach {
                it(items, exp, player, pos)
            }
        }
    }

    @Suppress("unused", "KotlinConstantConditions")
    private val listener = event.register { items, exp, player, pos ->
        if (!player.isAllowedToUseMagnetic()) return@register

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (config.animation.enabled) {
                    Animation.pullItemToPlayer(item, pos.center, player)
                    return@removeIf true
                }

                if (item.isEmpty) return@removeIf true
                val copiedStack = item.copy()
                if (!player.addItem(item)) {
                    tickInventoryAlert(player)
                    return@removeIf false
                }
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
        println("Dropped")
    }

    private val cooldowns: Map<Config.FullInventoryAlert.Alert, MutableMap<UUID, Instant>> = mapOf(
        config.fullInventoryAlert.soundAlert to mutableMapOf(),
        config.fullInventoryAlert.textAlert to mutableMapOf(),
        config.fullInventoryAlert.titleAlert to mutableMapOf()
    )
    private fun tickInventoryAlert(player: ServerPlayer) {
        val currentTime = Clock.System.now()
        cooldowns.forEach { (alert, playerCooldowns) ->
            if (!alert.enabled) return@forEach
            val lastAlert = playerCooldowns[player.uuid]
            if (lastAlert == null || currentTime > lastAlert + alert.cooldownInSeconds.seconds) {
                playerCooldowns[player.uuid] = currentTime
                alert.invoke(player)
            }
        }
    }
}

fun interface DropEventConsumer {
    operator fun invoke(items: MutableList<ItemStack>, exp: MutableInt, player: ServerPlayer, pos: BlockPos)
}