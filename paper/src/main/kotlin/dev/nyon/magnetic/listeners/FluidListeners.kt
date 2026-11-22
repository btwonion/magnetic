package dev.nyon.magnetic.listeners

import dev.nyon.magnetic.Animation
import dev.nyon.magnetic.DropEntityDispatcher
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.bukkit.Bukkit
import org.bukkit.Fluid
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val bucketHandledDropKey = NamespacedKey("magnetic", "bucket_handled")

/**
 * Tracks fluids placed from buckets and hands the drops they cause to the placing player.
 *
 * Every event here may fire on a different region thread on Folia, so the tracked fluids live in a
 * concurrent map and drops are always handed to the player's own scheduler instead of touching the
 * player from the fluid's region.
 */
@Suppress("unused")
object FluidListeners {

    private data class FluidKey(
        val worldId: UUID,
        val x: Int,
        val y: Int,
        val z: Int
    )

    private data class FluidHolder(
        val playerId: UUID,
        val placedAt: Long,
        val world: World,
        val ignoreDrops: Boolean = false
    )

    private val storedFluidHolders = ConcurrentHashMap<FluidKey, FluidHolder>()
    private val ignoredFluids = setOf(Fluid.EMPTY, Fluid.LAVA, Fluid.FLOWING_LAVA)

    private val bucketEmptyEvent = listen<PlayerBucketEmptyEvent> {
        if (isCancelled || !config.buckets.enabled) return@listen
        val fluidPos = block.location
        val holder = FluidHolder(player.uniqueId, System.currentTimeMillis(), fluidPos.world)

        // The fluid is placed after this event, so check it on the next tick of its region.
        Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, fluidPos) {
            if (ignoredFluids.contains(fluidPos.world.getFluidData(fluidPos).fluidType)) return@execute
            storedFluidHolders[fluidPos.key()] = holder
        }
    }

    private val waterSpreadEvent = listen<BlockFromToEvent> {
        val source = activeHolder(block.key()) ?: return@listen
        storedFluidHolders[toBlock.key()] = source.copy(ignoreDrops = toBlock.type.isIgnored)
    }

    private val itemSpawnEvent = listen<ItemSpawnEvent>(EventPriority.MONITOR) {
        if (isCancelled || !config.buckets.enabled) return@listen
        if (entity.persistentDataContainer.has(leafDecayHandledDropKey, PersistentDataType.BYTE)) return@listen
        if (entity.persistentDataContainer.has(bucketHandledDropKey, PersistentDataType.BYTE)) return@listen
        if (Animation.tracksItem(entity)) return@listen

        val itemPos = entity.location
        val holder = activeHolder(itemPos.key()) ?: return@listen
        if (holder.ignoreDrops) return@listen
        if (ignoredFluids.contains(itemPos.world.getFluidData(itemPos).fluidType)) return@listen
        if (Bukkit.getPlayer(holder.playerId) == null) return@listen

        entity.persistentDataContainer.set(bucketHandledDropKey, PersistentDataType.BYTE, 1.toByte())
        DropEntityDispatcher.claim(entity) { claimedDrop ->
            DropEntityDispatcher.dispatchWithCurrentConditions(claimedDrop, holder.playerId, bucketHandledDropKey)
        }
    }

    // Removes expired fluids and fluids that are no longer in place.
    private val cleanupTask = Main.INSTANCE.server.asyncScheduler.runAtFixedRate(
        Main.INSTANCE,
        {
            if (!config.buckets.enabled) {
                storedFluidHolders.clear()
                return@runAtFixedRate
            }
            storedFluidHolders.entries.removeIf { isExpired(it.value) }
        },
        500L,
        500L,
        TimeUnit.MILLISECONDS
    )

    private val fluidCheckTask = Main.INSTANCE.server.asyncScheduler.runAtFixedRate(
        Main.INSTANCE,
        {
            storedFluidHolders.forEach { (key, holder) ->
                val location = Location(holder.world, key.x.toDouble(), key.y.toDouble(), key.z.toDouble())
                Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, location) {
                    if (ignoredFluids.contains(holder.world.getFluidData(location).fluidType)) {
                        storedFluidHolders.remove(key, holder)
                    }
                }
            }
        },
        5L,
        5L,
        TimeUnit.SECONDS
    )

    private fun activeHolder(key: FluidKey): FluidHolder? {
        val holder = storedFluidHolders[key] ?: return null
        if (!isExpired(holder)) return holder
        storedFluidHolders.remove(key, holder)
        return null
    }

    // abilityTimeout is in milliseconds; a negative value keeps the fluid tracked until it disappears.
    private fun isExpired(holder: FluidHolder): Boolean {
        val timeout = config.buckets.abilityTimeout
        if (timeout < 0) return false
        return System.currentTimeMillis() - holder.placedAt > timeout
    }

    private fun Block.key() = FluidKey(world.uid, x, y, z)

    private fun Location.key() = FluidKey(world.uid, blockX, blockY, blockZ)
}
