package dev.nyon.magnetic.compat

import dev.nyon.magnetic.DropAuthorization
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.extensions.SingleListener
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import dev.nyon.magnetic.extensions.unregister
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.EntitySnapshot
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object TreeCapitatorCompat {
    private const val MARKER_DISCOVERY_RANGE = 24.0
    private const val MARKER_RANGE = 0.25
    private const val SESSION_TTL_TICKS = 40
    private const val TRIGGER_COLLECTION_DELAY_TICKS = 2L
    private val triggerItemKey = NamespacedKey("magnetic", "tree_capitator_trigger")
    private val handledDropKey = NamespacedKey("magnetic", "tree_capitator_handled")

    private data class BlockKey(val worldId: UUID, val x: Int, val y: Int, val z: Int) {
        fun distanceSquared(other: BlockKey): Double {
            if (worldId != other.worldId) return Double.POSITIVE_INFINITY
            val dx = (x - other.x).toDouble()
            val dy = (y - other.y).toDouble()
            val dz = (z - other.z).toDouble()
            return dx * dx + dy * dy + dz * dz
        }

        fun neighbors(): Sequence<BlockKey> = sequence {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue
                        yield(BlockKey(worldId, x + dx, y + dy, z + dz))
                    }
                }
            }
        }
    }

    private data class Session(
        val id: Long,
        val authorization: DropAuthorization,
        val root: BlockKey,
        val expiresAtTick: Int,
        var triggerClaimed: Boolean = false
    )

    private data class Cut(
        val id: Long,
        val authorization: DropAuthorization,
        val root: BlockKey,
        val markerPositions: MutableSet<BlockKey>,
        var expiresAtTick: Int
    )

    private data class ClaimedDrop(
        val location: Location,
        val itemStack: ItemStack,
        val entitySnapshot: EntitySnapshot
    )

    private val stateLock = Any()
    private val sessionsByWorld = mutableMapOf<UUID, MutableList<Session>>()
    private val cutsByWorld = mutableMapOf<UUID, MutableList<Cut>>()
    private val nextSessionId = AtomicLong()
    private var blockBreakListener: SingleListener<BlockBreakEvent>? = null
    private var blockDropItemListener: SingleListener<BlockDropItemEvent>? = null
    private var itemSpawnListener: SingleListener<ItemSpawnEvent>? = null

    @Volatile
    private var treeCapitatorEnabled = false

    fun listenForEvents() {
        listen<ServerLoadEvent> {
            refreshDatapackState()
        }
    }

    internal fun isTriggerItem(item: Item): Boolean {
        return item.persistentDataContainer.has(triggerItemKey)
    }

    private fun refreshDatapackState() {
        treeCapitatorEnabled = hasTreeCapitatorDatapack()
        if (!treeCapitatorEnabled) {
            synchronized(stateLock) {
                sessionsByWorld.clear()
                cutsByWorld.clear()
            }
            unregisterDropListeners()
            return
        }
        registerDropListeners()
    }

    private fun registerDropListeners() {
        if (blockBreakListener != null || blockDropItemListener != null || itemSpawnListener != null) return

        blockBreakListener = listen<BlockBreakEvent>(EventPriority.MONITOR) {
            if (!treeCapitatorEnabled || isCancelled || !block.type.isTreeCapitatorRoot() || block.type.isIgnored) {
                return@listen
            }
            val authorization = DropEventDispatcher.authorize(player) ?: return@listen
            val currentTick = Bukkit.getCurrentTick()
            val session = Session(
                nextSessionId.incrementAndGet(),
                authorization,
                block.location.toMagneticBlockKey(),
                currentTick + SESSION_TTL_TICKS
            )

            synchronized(stateLock) {
                cleanupLocked(currentTick)
                sessionsByWorld.getOrPut(session.root.worldId, ::mutableListOf).add(session)
            }
        }

        blockDropItemListener = listen<BlockDropItemEvent>(EventPriority.LOWEST) {
            if (!treeCapitatorEnabled || isCancelled) return@listen
            val triggerItem = items.firstOrNull { it.itemStack.type == blockState.type } ?: return@listen
            val root = blockState.location.toMagneticBlockKey()
            val session = synchronized(stateLock) {
                cleanupLocked(Bukkit.getCurrentTick())
                sessionsByWorld[root.worldId]
                    ?.lastOrNull {
                        !it.triggerClaimed &&
                            it.root == root &&
                            it.authorization.playerId == player.uniqueId
                    }
                    ?.also { it.triggerClaimed = true }
            } ?: return@listen

            triggerItem.persistentDataContainer.set(triggerItemKey, PersistentDataType.LONG, session.id)
            collectTriggerAfterDatapackTick(triggerItem, session.authorization)
        }

        itemSpawnListener = listen<ItemSpawnEvent>(EventPriority.MONITOR) {
            if (isCancelled || !treeCapitatorEnabled || entity.persistentDataContainer.has(handledDropKey)) {
                return@listen
            }
            val itemKey = location.toMagneticBlockKey()
            if (!hasPendingCut(itemKey.worldId)) return@listen

            val authorization = resolveCut(entity, itemKey) ?: return@listen
            if (Bukkit.getPlayer(authorization.playerId) == null) return@listen

            entity.persistentDataContainer.set(handledDropKey, PersistentDataType.BYTE, 1.toByte())
            claimDrop(entity) { claimedDrop ->
                dispatchClaimedDrop(claimedDrop, authorization)
            }
        }
    }

    private fun unregisterDropListeners() {
        blockBreakListener?.unregister()
        blockDropItemListener?.unregister()
        itemSpawnListener?.unregister()
        blockBreakListener = null
        blockDropItemListener = null
        itemSpawnListener = null
    }

    private fun collectTriggerAfterDatapackTick(item: Item, authorization: DropAuthorization) {
        item.scheduler.execute(Main.INSTANCE, {
            if (!item.isValid || !item.persistentDataContainer.has(triggerItemKey)) return@execute
            item.persistentDataContainer.remove(triggerItemKey)
            item.persistentDataContainer.set(handledDropKey, PersistentDataType.BYTE, 1.toByte())
            claimDrop(item) { claimedDrop ->
                dispatchClaimedDrop(claimedDrop, authorization)
            }
        }, null, TRIGGER_COLLECTION_DELAY_TICKS)
    }

    private fun claimDrop(entity: Item, onClaimed: (ClaimedDrop) -> Unit) {
        entity.scheduler.execute(Main.INSTANCE, {
            if (!entity.isValid) return@execute

            val entitySnapshot = entity.createSnapshot() ?: return@execute
            val claimedDrop = ClaimedDrop(
                entity.location.clone(),
                entity.itemStack.clone(),
                entitySnapshot
            )
            entity.remove()
            onClaimed(claimedDrop)
        }, null, 0L)
    }

    private fun dispatchClaimedDrop(claimedDrop: ClaimedDrop, authorization: DropAuthorization) {
        val player = Bukkit.getPlayer(authorization.playerId)
        if (player == null) {
            restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack))
            return
        }

        val restored = AtomicBoolean()
        val restoreOnFailure = {
            if (restored.compareAndSet(false, true)) {
                restoreResidualDrops(claimedDrop, listOf(claimedDrop.itemStack))
            }
        }
        val scheduled = player.scheduler.execute(Main.INSTANCE, {
            if (player.world.uid != claimedDrop.location.world.uid) {
                restoreOnFailure()
                return@execute
            }

            val itemStacks = mutableListOf(claimedDrop.itemStack.clone())
            DropEventDispatcher.callAuthorized(
                DropEvent(itemStacks, MutableInt(), player, claimedDrop.location, handledDropKey),
                authorization
            )
            restoreResidualDrops(claimedDrop, itemStacks)
        }, restoreOnFailure, 0L)
        if (!scheduled) restoreOnFailure()
    }

    private fun restoreResidualDrops(claimedDrop: ClaimedDrop, items: List<ItemStack>) {
        val residual = items
            .filter { !it.type.isAir && it.amount > 0 }
            .map(ItemStack::clone)
        if (residual.isEmpty()) return

        Main.INSTANCE.server.regionScheduler.execute(Main.INSTANCE, claimedDrop.location) {
            residual.forEach { restoreDropWithoutEvent(claimedDrop, it) }
        }
    }

    private fun restoreDropWithoutEvent(claimedDrop: ClaimedDrop, itemStack: ItemStack) {
        val world = claimedDrop.location.world as CraftWorld
        val item = claimedDrop.entitySnapshot.createEntity(world) as Item
        val handle = (item as CraftEntity).handle
        handle.setPos(claimedDrop.location.x, claimedDrop.location.y, claimedDrop.location.z)
        item.itemStack = itemStack
        item.persistentDataContainer.set(handledDropKey, PersistentDataType.BYTE, 1.toByte())

        // Other plugins already observed the original spawn, so restore without firing it twice.
        world.addEntityToWorld(handle, null)
    }

    private fun hasPendingCut(worldId: UUID): Boolean {
        return synchronized(stateLock) {
            cleanupLocked(Bukkit.getCurrentTick())
            !sessionsByWorld[worldId].isNullOrEmpty() || !cutsByWorld[worldId].isNullOrEmpty()
        }
    }

    private fun resolveCut(item: Item, itemKey: BlockKey): DropAuthorization? {
        val currentTick = Bukkit.getCurrentTick()
        synchronized(stateLock) {
            cleanupLocked(currentTick)
            cutsByWorld[itemKey.worldId]
                ?.firstOrNull { itemKey in it.markerPositions }
                ?.let { cut ->
                    cut.expiresAtTick = currentTick + SESSION_TTL_TICKS
                    return cut.authorization
                }
        }

        if (!item.hasTreeCapitatorMarker()) return null
        val markerComponent = item.discoverMarkerComponent(itemKey)
        if (markerComponent.isEmpty()) return null

        return synchronized(stateLock) {
            cleanupLocked(currentTick)
            findConnectedCutLocked(itemKey.worldId, markerComponent)
                ?.also { extendCutLocked(it, markerComponent, currentTick) }
                ?.authorization
                ?: bindPendingSessionLocked(itemKey.worldId, markerComponent, currentTick)?.authorization
                ?: extendNearestCutLocked(itemKey.worldId, markerComponent, currentTick)?.authorization
        }
    }

    private fun findConnectedCutLocked(worldId: UUID, markerComponent: Set<BlockKey>): Cut? {
        return cutsByWorld[worldId]
            ?.filter { cut ->
                markerComponent.any { marker ->
                    marker in cut.markerPositions || marker.neighbors().any(cut.markerPositions::contains)
                }
            }
            ?.maxByOrNull { it.id }
    }

    private fun bindPendingSessionLocked(
        worldId: UUID,
        markerComponent: Set<BlockKey>,
        currentTick: Int
    ): Cut? {
        val sessions = sessionsByWorld[worldId] ?: return null
        val session = sessions
            .asSequence()
            .map { candidate -> candidate to markerComponent.minOf(candidate.root::distanceSquared) }
            .filter { (_, distance) -> distance <= MARKER_DISCOVERY_RANGE * MARKER_DISCOVERY_RANGE }
            .minWithOrNull(
                compareBy<Pair<Session, Double>> { (_, distance) -> distance }
                    .thenByDescending { (candidate) -> candidate.id }
            )
            ?.first
            ?: return null

        sessions.remove(session)
        if (sessions.isEmpty()) sessionsByWorld.remove(worldId)
        return Cut(
            session.id,
            session.authorization,
            session.root,
            markerComponent.toMutableSet(),
            currentTick + SESSION_TTL_TICKS
        ).also { cutsByWorld.getOrPut(worldId, ::mutableListOf).add(it) }
    }

    private fun extendNearestCutLocked(
        worldId: UUID,
        markerComponent: Set<BlockKey>,
        currentTick: Int
    ): Cut? {
        val cut = cutsByWorld[worldId]
            ?.minWithOrNull(
                compareBy<Cut> { candidate ->
                    markerComponent.minOf { marker ->
                        candidate.markerPositions.minOf(marker::distanceSquared)
                    }
                }.thenByDescending { it.id }
            )
            ?: return null
        extendCutLocked(cut, markerComponent, currentTick)
        return cut
    }

    private fun extendCutLocked(cut: Cut, markerComponent: Set<BlockKey>, currentTick: Int) {
        cut.markerPositions.addAll(markerComponent)
        cut.expiresAtTick = currentTick + SESSION_TTL_TICKS
    }

    private fun Item.discoverMarkerComponent(itemKey: BlockKey): Set<BlockKey> {
        val markerPositions = getNearbyEntities(
            MARKER_DISCOVERY_RANGE,
            MARKER_DISCOVERY_RANGE,
            MARKER_DISCOVERY_RANGE
        ).asSequence()
            .filter { it.isTreeCapitatorMarker() }
            .map { it.location.toMagneticBlockKey() }
            .toSet()
        if (itemKey !in markerPositions) return emptySet()

        val component = mutableSetOf(itemKey)
        val pending = ArrayDeque<BlockKey>().also { it.add(itemKey) }
        while (pending.isNotEmpty()) {
            pending.removeFirst().neighbors().forEach { neighbor ->
                if (neighbor in markerPositions && component.add(neighbor)) pending.add(neighbor)
            }
        }
        return component
    }

    private fun Item.hasTreeCapitatorMarker(): Boolean {
        return getNearbyEntities(MARKER_RANGE, MARKER_RANGE, MARKER_RANGE)
            .any { it.isTreeCapitatorMarker() }
    }

    private fun Entity.isTreeCapitatorMarker(): Boolean {
        return type == EntityType.MARKER && scoreboardTags.any { it == "TC_Log" || it == "TC_Leaf" }
    }

    private fun hasTreeCapitatorDatapack(): Boolean {
        return Bukkit.getDatapackManager().enabledPacks.any { pack ->
            val name = pack.name.lowercase()
            val title = PlainTextComponentSerializer.plainText().serialize(pack.title).lowercase()
            val description = PlainTextComponentSerializer.plainText().serialize(pack.description).lowercase()
            name.contains("treecapitator") ||
                title.contains("treecapitator") ||
                name.contains("tree_capitator") ||
                title.contains("tree capitator") ||
                description.contains("cut trees in one go")
        }
    }

    private fun Material.isTreeCapitatorRoot(): Boolean {
        return name.endsWith("_LOG") || name.endsWith("_STEM")
    }

    private fun cleanupLocked(currentTick: Int) {
        sessionsByWorld.entries.removeIf { (_, sessions) ->
            sessions.removeIf { currentTick - it.expiresAtTick >= 0 }
            sessions.isEmpty()
        }
        cutsByWorld.entries.removeIf { (_, cuts) ->
            cuts.removeIf { currentTick - it.expiresAtTick >= 0 }
            cuts.isEmpty()
        }
    }

    private fun Location.toMagneticBlockKey(): BlockKey {
        return BlockKey(world.uid, blockX, blockY, blockZ)
    }
}
