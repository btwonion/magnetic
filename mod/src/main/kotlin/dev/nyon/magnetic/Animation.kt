package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.utils.MixinHelper
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object Animation {
    private val blocksPerTick = config.animation.blocksPerSecond / 20
    private val trackedItemEntities = ConcurrentHashMap<ItemEntity, UUID>()

    fun pullItemToPlayer(item: ItemStack, pos: Vec3, player: ServerPlayer): ItemEntity {
        val itemEntity = ItemEntity(player.level(), pos.x, pos.y, pos.z, item)
        if (!config.animation.canOtherPlayersPickup) itemEntity.setTarget(player.uuid)
        MixinHelper.animationSkip.set(true)
        val spawned = try {
            player.level().addFreshEntity(itemEntity)
        } finally {
            MixinHelper.animationSkip.remove()
        }
        if (spawned) trackedItemEntities[itemEntity] = player.uuid
        return itemEntity
    }

    fun tick() {
        trackedItemEntities.forEach { (itemEntity, targetId) ->
            if (!itemEntity.isAlive) {
                untrackEntity(itemEntity, targetId)
                return@forEach
            }

            val target = itemEntity.level().server?.playerList?.getPlayer(targetId)
            if (target == null || !target.isAlive || target.level() !== itemEntity.level()) {
                untrackEntity(itemEntity, targetId)
                return@forEach
            }

            val vec = target.position().subtract(itemEntity.position())
            val length = vec.length()
            if (length == 0.0) return@forEach

            val tickPart = blocksPerTick / length
            val tickVec = vec.multiply(
                tickPart,
                if (itemEntity.horizontalCollision) tickPart * 2 else tickPart,
                tickPart
            )
            itemEntity.addDeltaMovement(tickVec)
        }
    }

    fun invokePickupItemEntity(itemEntity: ItemEntity) {
        trackedItemEntities.remove(itemEntity)
    }

    fun tracksItem(itemEntity: ItemEntity) = trackedItemEntities.containsKey(itemEntity)

    private fun untrackEntity(itemEntity: ItemEntity, targetId: UUID) {
        trackedItemEntities.remove(itemEntity, targetId)
    }
}
