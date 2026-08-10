package dev.nyon.magnetic

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.listen
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.util.concurrent.ConcurrentHashMap

object Animation {
    private val blocksPerTick = config.animation.blocksPerSecond / 20
    private val trackedItemEntities = ConcurrentHashMap<Item, Player>()

    fun pullItemToPlayer(
        item: ItemStack,
        pos: Location,
        player: Player,
        itemSpawnMarker: NamespacedKey? = null
    ) {
        val spawnPos = pos.clone()
        val ownerId = player.playerProfile.id
        Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, spawnPos) {
            spawnPos.world.dropItem(spawnPos, item) { itemEntity ->
                if (itemSpawnMarker != null) {
                    itemEntity.persistentDataContainer.set(
                        itemSpawnMarker,
                        PersistentDataType.BYTE,
                        1.toByte()
                    )
                }
                if (!config.animation.canOtherPlayersPickup) {
                    val entityScheduler = itemEntity.scheduler
                    entityScheduler.execute(Main.INSTANCE, {
                        itemEntity.owner = ownerId
                    }, null, 0L)
                }
                trackedItemEntities[itemEntity] = player
            }
        }
    }

    private val tickListener = listen<ServerTickStartEvent> {
        trackedItemEntities.forEach(::tickItem)
    }

    private fun tickItem(itemEntity: Item, target: Player) {
        val targetScheduled = target.scheduler.execute(Main.INSTANCE, {
            val targetPos = target.location.clone()
            val itemScheduled = itemEntity.scheduler.execute(Main.INSTANCE, {
                val itemEntityPos = itemEntity.location
                if (targetPos.world.uid != itemEntityPos.world.uid) {
                    untrackEntity(itemEntity)
                    return@execute
                }
                val mcEntity = (itemEntity as CraftEntity).handle

                val vec = targetPos.subtract(itemEntityPos).toVector()
                val length = vec.length()
                if (length == 0.0) return@execute
                val tickPart = blocksPerTick / length
                val tickVec = Vector(
                    vec.x * tickPart,
                    vec.y * (if (mcEntity.horizontalCollision) tickPart * 2 else tickPart),
                    vec.z * tickPart
                )
                itemEntity.velocity = itemEntity.velocity.add(tickVec)
            }, { untrackEntity(itemEntity) }, 0L)
            if (!itemScheduled) untrackEntity(itemEntity)
        }, { untrackEntity(itemEntity) }, 0L)
        if (!targetScheduled) untrackEntity(itemEntity)
    }

    private val playerPickupItemListener = listen<PlayerAttemptPickupItemEvent>(EventPriority.HIGHEST) {
        untrackEntity(item)
    }

    private fun untrackEntity(item: Item) {
        trackedItemEntities.remove(item)
    }

    fun tracksItem(item: Item) = trackedItemEntities.containsKey(item)
}
