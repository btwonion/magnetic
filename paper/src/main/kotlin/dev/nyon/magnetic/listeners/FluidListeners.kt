@file:OptIn(ExperimentalTime::class)

package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.Animation
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.listen
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Fluid
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.util.Vector
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("unused")
object FluidListeners {

    private val fluidTrackingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fluidHolderMutex = Mutex()
    private val storedFluidHolders = mutableMapOf<Vector, FluidHolder>()
    private val ignoredFluids = setOf(Fluid.EMPTY, Fluid.LAVA, Fluid.FLOWING_LAVA)
    private val regionScheduler = Main.INSTANCE.server.regionScheduler

    private val bucketEmptyEvent = listen<PlayerBucketEmptyEvent> {
        val fluidPos = block.location

        regionScheduler.execute(Main.INSTANCE, fluidPos) {
            val fluidData = fluidPos.world.getFluidData(fluidPos)
            fluidTrackingScope.launch {
                if (ignoredFluids.contains(fluidData.fluidType)) return@launch
                fluidHolderMutex.withLock {
                    storedFluidHolders[fluidPos.toVector()] = FluidHolder(player, Clock.System.now(), fluidPos.world)
                }
            }
        }
    }

    private val waterSpreadEvent = listen<BlockFromToEvent> {
        val sourcePos = block.location.toVector()
        if (!storedFluidHolders.containsKey(sourcePos)) return@listen
        val toPos = toBlock.location.toVector()

        fluidTrackingScope.launch {
            fluidHolderMutex.withLock {
                storedFluidHolders[toPos] = storedFluidHolders[sourcePos]!!
            }
        }
    }

    private val itemSpawnEvent = listen<ItemSpawnEvent> {
        val itemPos = entity.location
        if (Animation.tracksItem(entity)) return@listen
        regionScheduler.execute(Main.INSTANCE, itemPos) {
            val itemFluidData = itemPos.world.getFluidData(itemPos)
            if (ignoredFluids.contains(itemFluidData.fluidType)) return@execute

            val holder =
                runBlocking { fluidHolderMutex.withLock { storedFluidHolders[itemPos.toBlockLocation().toVector()] } } ?: return@execute

            val itemStacks = mutableListOf(entity.itemStack)
            DropEvent(itemStacks, MutableInt(), holder.player, itemPos).also(Event::callEvent)

            if (itemStacks.isEmpty()) {
                entity.scheduler.execute(Main.INSTANCE, { entity.remove() }, null, 1)
            }
        }
    }

    // Scheduler that removes all expired data after a given time
    private val removalScheduler = fluidTrackingScope.launch {
        while (config.buckets.enabled && config.buckets.abilityTimeout > -1) {
            fluidHolderMutex.withLock {
                storedFluidHolders.filterValues { it.placedAt < Clock.System.now() - config.buckets.abilityTimeout.seconds }
                    .forEach { storedFluidHolders.remove(it.key) }
            }

            delay(0.5.seconds)
        }
    }

    // Scheduler that checks periodically if a fluid is still in place
    private val checkScheduler = fluidTrackingScope.launch {
        while (config.buckets.enabled) {
            fluidHolderMutex.withLock {
                storedFluidHolders.forEach { (vec, holder) ->
                    val loc = vec.toLocation(holder.world)
                    regionScheduler.execute(Main.INSTANCE, loc) {
                        if (ignoredFluids.contains(holder.world.getFluidData(loc).fluidType)) {
                            storedFluidHolders.remove(vec)
                        }
                    }
                }
            }

            delay(5.seconds)
        }
    }
}

data class FluidHolder(val player: Player, val placedAt: Instant, val world: World)