package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

object Animation {
    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val blocksPerTick = config.animation.blocksPerSecond / 20
    private val trackedItemEntities = mutableMapOf<ItemEntity, ServerPlayer>()
    private val trackedItemEntitiesMutex = Mutex()

    fun pullItemToPlayer(item: ItemStack, pos: Vec3, player: ServerPlayer) {
        val itemEntity = ItemEntity(player.level(), pos.x, pos.y, pos.z, item)
        if (!config.animation.canOtherPlayersPickup) itemEntity.setTarget(player.uuid)
        player.level().addFreshEntity(itemEntity)
        animationScope.launch {
            trackedItemEntitiesMutex.withLock {
                trackedItemEntities[itemEntity] = player
            }
        }
    }

    private val tickListener = ServerTickEvents.END_WORLD_TICK.register { level ->
        animationScope.launch {
            val copiedItemEntities: Map<ItemEntity, ServerPlayer>
            trackedItemEntitiesMutex.withLock {
                copiedItemEntities = trackedItemEntities.toMap()
            }

            copiedItemEntities.forEach { (itemEntity, target) ->
                val targetPos = target.position()
                val itemEntityPos = itemEntity.position()

                val vec = targetPos.subtract(itemEntityPos)
                val length = vec.length()
                val tickPart = blocksPerTick / length
                val tickVec = vec.multiply(
                    tickPart, if (itemEntity.horizontalCollision) tickPart * 2 else tickPart, tickPart
                )
                itemEntity.addDeltaMovement(tickVec)
            }
        }
    }

    fun invokePickupItemEntity(itemEntity: ItemEntity) {
        animationScope.launch {
            trackedItemEntitiesMutex.withLock {
                if (trackedItemEntities.containsKey(itemEntity)) trackedItemEntities.remove(itemEntity)
            }
        }
    }
}