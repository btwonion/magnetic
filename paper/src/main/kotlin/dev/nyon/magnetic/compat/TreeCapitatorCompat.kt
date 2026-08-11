package dev.nyon.magnetic.compat

import dev.nyon.magnetic.DropAuthorization
import dev.nyon.magnetic.DropEntityDispatcher
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.extensions.SingleListener
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import dev.nyon.magnetic.extensions.unregister
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object TreeCapitatorCompat {
    private const val MARKER_DISCOVERY_RANGE = 24.0
    private const val MARKER_RANGE = 0.25
    private const val SESSION_TTL_TICKS = 40
    private const val TRIGGER_COLLECTION_DELAY_TICKS = 2L
    private val triggerItemKey = NamespacedKey("magnetic", "tree_capitator_trigger")
    private val handledDropKey = NamespacedKey("magnetic", "tree_capitator_handled")

    private data class PendingSession(
        val id: Long,
        val authorization: DropAuthorization,
        val root: TreeCapitatorBlockKey,
        val expiresAtTick: Int
    )

    private data class ActiveBatch(
        val authorization: DropAuthorization,
        val worldId: UUID,
        val sentinel: Entity
    )

    private val stateLock = Any()
    private val sessionsByWorld = mutableMapOf<UUID, MutableList<PendingSession>>()
    private val activeBatches = ConcurrentHashMap<Thread, ActiveBatch>()
    private val nextSessionId = AtomicLong()
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
            }
            activeBatches.clear()
            unregisterDropListeners()
            return
        }
        registerDropListeners()
    }

    private fun registerDropListeners() {
        if (blockDropItemListener != null || itemSpawnListener != null) return

        blockDropItemListener = listen<BlockDropItemEvent>(EventPriority.LOWEST) {
            if (
                !treeCapitatorEnabled ||
                isCancelled ||
                !blockState.type.isTreeCapitatorRoot() ||
                blockState.type.isIgnored
            ) {
                return@listen
            }

            val triggerItem = items.firstOrNull { it.itemStack.type == blockState.type } ?: return@listen
            val authorization = DropEventDispatcher.authorize(player) ?: return@listen
            val currentTick = Bukkit.getCurrentTick()
            val session = PendingSession(
                nextSessionId.incrementAndGet(),
                authorization,
                blockState.location.toMagneticBlockKey(),
                currentTick + SESSION_TTL_TICKS
            )

            synchronized(stateLock) {
                cleanupSessionsLocked(currentTick)
                sessionsByWorld.getOrPut(session.root.worldId, ::mutableListOf).add(session)
            }

            triggerItem.persistentDataContainer.set(triggerItemKey, PersistentDataType.LONG, session.id)
            collectTriggerAfterDatapackTick(triggerItem, session)
        }

        itemSpawnListener = listen<ItemSpawnEvent>(EventPriority.MONITOR) {
            if (isCancelled || !treeCapitatorEnabled || entity.persistentDataContainer.has(handledDropKey)) {
                return@listen
            }

            val worldId = location.world.uid
            if (!hasPendingBatch(worldId)) return@listen
            val marker = entity.findTreeCapitatorMarker() ?: return@listen
            val authorization = resolveBatch(entity, marker) ?: return@listen
            if (Bukkit.getPlayer(authorization.playerId) == null) return@listen

            entity.persistentDataContainer.set(handledDropKey, PersistentDataType.BYTE, 1.toByte())
            DropEntityDispatcher.claim(entity) { claimedDrop ->
                DropEntityDispatcher.dispatch(claimedDrop, authorization, handledDropKey)
            }
        }
    }

    private fun unregisterDropListeners() {
        blockDropItemListener?.unregister()
        itemSpawnListener?.unregister()
        blockDropItemListener = null
        itemSpawnListener = null
    }

    private fun collectTriggerAfterDatapackTick(item: Item, session: PendingSession) {
        val expireSession = { removeSession(session) }
        val scheduled = item.scheduler.execute(Main.INSTANCE, {
            expireSession()
            if (!item.isValid || !item.persistentDataContainer.has(triggerItemKey)) return@execute

            item.persistentDataContainer.remove(triggerItemKey)
            item.persistentDataContainer.set(handledDropKey, PersistentDataType.BYTE, 1.toByte())
            DropEntityDispatcher.claim(item) { claimedDrop ->
                DropEntityDispatcher.dispatch(claimedDrop, session.authorization, handledDropKey)
            }
        }, expireSession, TRIGGER_COLLECTION_DELAY_TICKS)
        if (!scheduled) expireSession()
    }

    private fun hasPendingBatch(worldId: UUID): Boolean {
        if (activeAuthorization(worldId) != null) return true

        return synchronized(stateLock) {
            cleanupSessionsLocked(Bukkit.getCurrentTick())
            !sessionsByWorld[worldId].isNullOrEmpty()
        }
    }

    private fun resolveBatch(item: Item, marker: Entity): DropAuthorization? {
        val worldId = item.world.uid
        activeAuthorization(worldId)?.let { return it }

        val markerPositions = item.getNearbyEntities(
            MARKER_DISCOVERY_RANGE,
            MARKER_DISCOVERY_RANGE,
            MARKER_DISCOVERY_RANGE
        ).asSequence()
            .filter { it.isTreeCapitatorMarker() }
            .map { it.location.toMagneticBlockKey() }
            .toMutableSet()
            .also { it.add(marker.location.toMagneticBlockKey()) }

        val session = synchronized(stateLock) {
            val currentTick = Bukkit.getCurrentTick()
            cleanupSessionsLocked(currentTick)
            val sessions = sessionsByWorld[worldId] ?: return@synchronized null
            val sessionIndex = selectTreeCapitatorSessionIndex(
                sessions.map(PendingSession::root),
                markerPositions
            ) ?: return@synchronized null

            sessions.removeAt(sessionIndex).also {
                if (sessions.isEmpty()) sessionsByWorld.remove(worldId)
            }
        } ?: return null

        activateBatch(marker, session.authorization)
        return session.authorization
    }

    private fun activeAuthorization(worldId: UUID): DropAuthorization? {
        val thread = Thread.currentThread()
        val activeBatch = activeBatches[thread] ?: return null
        if (activeBatch.worldId == worldId && activeBatch.sentinel.isValid) {
            return activeBatch.authorization
        }

        activeBatches.remove(thread, activeBatch)
        return null
    }

    private fun activateBatch(marker: Entity, authorization: DropAuthorization) {
        val ownerThread = Thread.currentThread()
        val activeBatch = ActiveBatch(authorization, marker.world.uid, marker)
        activeBatches[ownerThread] = activeBatch

        val clearBatch = {
            activeBatches.remove(ownerThread, activeBatch)
            Unit
        }
        val scheduled = marker.scheduler.execute(Main.INSTANCE, clearBatch, clearBatch, 1L)
        if (!scheduled) clearBatch()
    }

    private fun removeSession(session: PendingSession) {
        synchronized(stateLock) {
            val sessions = sessionsByWorld[session.root.worldId] ?: return
            sessions.remove(session)
            if (sessions.isEmpty()) sessionsByWorld.remove(session.root.worldId)
        }
    }

    private fun Item.findTreeCapitatorMarker(): Entity? {
        return getNearbyEntities(MARKER_RANGE, MARKER_RANGE, MARKER_RANGE)
            .firstOrNull { it.isTreeCapitatorMarker() }
    }

    private fun Entity.isTreeCapitatorMarker(): Boolean {
        return type == EntityType.MARKER &&
            (scoreboardTags.contains("TC_Log") || scoreboardTags.contains("TC_Leaf"))
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

    private fun cleanupSessionsLocked(currentTick: Int) {
        sessionsByWorld.entries.removeIf { (_, sessions) ->
            sessions.removeIf { currentTick - it.expiresAtTick >= 0 }
            sessions.isEmpty()
        }
    }

    private fun Location.toMagneticBlockKey(): TreeCapitatorBlockKey {
        return TreeCapitatorBlockKey(world.uid, blockX, blockY, blockZ)
    }
}
