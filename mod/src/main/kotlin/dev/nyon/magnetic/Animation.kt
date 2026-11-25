package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.utils.MixinHelper
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object Animation {
    /**
     * How long a pulled item may stay within the target's pickup reach without being collected. Pickup filters
     * and other mods can refuse an item, which would otherwise follow the player until it despawns.
     */
    const val RELEASE_AFTER_TICKS = 20

    private val blocksPerTick = config.animation.blocksPerSecond / 20
    private val trackedItemEntities = ConcurrentHashMap<ItemEntity, TrackedItem>()

    private class TrackedItem(val target: UUID) {
        var ticksInReach = 0
    }

    fun pullItemToPlayer(item: ItemStack, pos: Vec3, player: ServerPlayer): ItemEntity {
        val itemEntity = ItemEntity(player.level(), pos.x, pos.y, pos.z, item)
        if (!config.animation.canOtherPlayersPickup) itemEntity.setTarget(player.uuid)
        MixinHelper.animationSkip.set(true)
        val spawned = try {
            player.level().addFreshEntity(itemEntity)
        } finally {
            MixinHelper.animationSkip.remove()
        }
        if (spawned) trackedItemEntities[itemEntity] = TrackedItem(player.uuid)
        return itemEntity
    }

    /**
     * Moves tracked items toward their targets. Pass the ticking [level] when the loader ticks levels separately,
     * so each item moves once per server tick; `null` processes every level.
     */
    @JvmOverloads
    fun tick(level: Level? = null) {
        trackedItemEntities.forEach { (itemEntity, tracked) ->
            if (level != null && itemEntity.level() !== level) return@forEach
            if (!itemEntity.isAlive) {
                untrackEntity(itemEntity, tracked)
                return@forEach
            }

            val target = itemEntity.level().server?.playerList?.getPlayer(tracked.target)
            if (target == null || !target.isAlive || target.level() !== itemEntity.level()) {
                untrackEntity(itemEntity, tracked)
                return@forEach
            }

            // Players collect items intersecting this box, see Player.aiStep.
            if (itemEntity.boundingBox.intersects(target.boundingBox.inflate(1.0, 0.5, 1.0))) {
                if (++tracked.ticksInReach >= RELEASE_AFTER_TICKS) release(itemEntity, tracked)
                return@forEach
            }
            tracked.ticksInReach = 0

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

    private fun release(itemEntity: ItemEntity, tracked: TrackedItem) {
        if (!trackedItemEntities.remove(itemEntity, tracked)) return
        itemEntity.setTarget(null)
    }

    private fun untrackEntity(itemEntity: ItemEntity, tracked: TrackedItem) {
        trackedItemEntities.remove(itemEntity, tracked)
    }
}
