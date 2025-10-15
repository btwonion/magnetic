package dev.nyon.magnetic

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.listen
import io.papermc.paper.threadedregions.EntityScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

object Animation {
    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val blocksPerTick = config.animation.blocksPerSecond / 20
    private val trackedItemEntities = mutableMapOf<Item, Player>()
    private val trackedItemEntitiesMutex = Mutex()

    fun pullItemToPlayer(item: ItemStack, pos: Location, player: Player) {
        player.world.dropItem(pos, item) { itemEntity ->
            if (!config.animation.canOtherPlayersPickup) {
                val entityScheduler = itemEntity.scheduler
                entityScheduler.execute(Main.INSTANCE, {
                    itemEntity.owner = player.playerProfile.id
                }, null, 0L)
            }
            animationScope.launch {
                trackedItemEntitiesMutex.withLock {
                    trackedItemEntities[itemEntity] = player
                }
            }
        }
    }

    private val tickListener = listen<ServerTickStartEvent> {
        animationScope.launch {
            val copiedItemEntities: Map<Item, Player>
            trackedItemEntitiesMutex.withLock {
                copiedItemEntities = trackedItemEntities.toMap()
            }

            copiedItemEntities.forEach { (itemEntity, target) ->
                itemEntity.scheduler.execute(Main.INSTANCE, {
                    val targetPos = target.location
                    val itemEntityPos = itemEntity.location
                    val mcEntity = (itemEntity as CraftEntity).handle

                    val vec = targetPos.subtract(itemEntityPos).toVector()
                    val length = vec.length()
                    val tickPart = blocksPerTick / length
                    val tickVec = Vector(
                        vec.x * tickPart,
                        vec.y * (if (mcEntity.horizontalCollision) tickPart * 2 else tickPart),
                        vec.z * tickPart
                    )
                    itemEntity.velocity = itemEntity.velocity.add(tickVec)
                }, null, 0L)
            }
        }
    }

    private val playerPickupItemListener = listen<PlayerAttemptPickupItemEvent>(EventPriority.HIGHEST) {
        animationScope.launch {
            trackedItemEntitiesMutex.withLock {
                if (trackedItemEntities.containsKey(item)) trackedItemEntities.remove(item)
            }
        }
    }
}