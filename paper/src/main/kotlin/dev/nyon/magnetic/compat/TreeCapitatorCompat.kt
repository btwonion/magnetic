package dev.nyon.magnetic.compat

import dev.nyon.magnetic.DropAuthorization
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.Main
import dev.nyon.magnetic.extensions.SingleListener
import dev.nyon.magnetic.extensions.listen
import dev.nyon.magnetic.extensions.unregister
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object TreeCapitatorCompat {
    private const val SESSION_RANGE = 24.0
    private const val MARKER_RANGE = 0.25
    private const val SESSION_TTL_MS = 1500L
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
    }

    private data class Session(
        val id: Long,
        val authorization: DropAuthorization,
        val root: BlockKey,
        val expiresAt: Long,
        var triggerClaimed: Boolean = false
    )

    private data class Cut(
        val authorization: DropAuthorization,
        val markerPositions: Set<BlockKey>,
        val expiresAt: Long
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
            if (!treeCapitatorEnabled || isCancelled || !block.type.isTreeCapitatorRoot()) return@listen
            val authorization = DropEventDispatcher.authorize(player) ?: return@listen
            val now = System.currentTimeMillis()
            val session = Session(
                nextSessionId.incrementAndGet(),
                authorization,
                block.location.toMagneticBlockKey(),
                now + SESSION_TTL_MS
            )

            synchronized(stateLock) {
                cleanupLocked(now)
                sessionsByWorld.getOrPut(session.root.worldId, ::mutableListOf).add(session)
            }
        }

        blockDropItemListener = listen<BlockDropItemEvent>(EventPriority.LOWEST) {
            if (!treeCapitatorEnabled) return@listen
            val triggerItem = items.firstOrNull { it.itemStack.type == blockState.type } ?: return@listen
            val root = blockState.location.toMagneticBlockKey()
            val session = synchronized(stateLock) {
                cleanupLocked(System.currentTimeMillis())
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

        itemSpawnListener = listen<ItemSpawnEvent>(EventPriority.HIGHEST) {
            if (!treeCapitatorEnabled || entity.persistentDataContainer.has(handledDropKey)) return@listen
            val itemKey = location.toMagneticBlockKey()
            if (!hasPendingCut(itemKey.worldId)) return@listen

            val authorization = findBoundCut(itemKey) ?: discoverAndBindCut(entity, itemKey) ?: return@listen
            val player = Bukkit.getPlayer(authorization.playerId) ?: return@listen
            val itemStacks = mutableListOf(entity.itemStack)
            DropEventDispatcher.callAuthorized(
                DropEvent(itemStacks, MutableInt(), player, location, handledDropKey),
                authorization
            )

            if (itemStacks.isEmpty()) {
                isCancelled = true
                entity.remove()
            } else {
                entity.itemStack = itemStacks.first()
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
            val player = Bukkit.getPlayer(authorization.playerId) ?: return@execute
            val itemStacks = mutableListOf(item.itemStack)
            DropEventDispatcher.callAuthorized(
                DropEvent(itemStacks, MutableInt(), player, item.location, handledDropKey),
                authorization
            )

            if (itemStacks.isEmpty()) {
                item.remove()
            } else {
                item.itemStack = itemStacks.first()
            }
        }, null, TRIGGER_COLLECTION_DELAY_TICKS)
    }

    private fun hasPendingCut(worldId: UUID): Boolean {
        return synchronized(stateLock) {
            cleanupLocked(System.currentTimeMillis())
            !sessionsByWorld[worldId].isNullOrEmpty() || !cutsByWorld[worldId].isNullOrEmpty()
        }
    }

    private fun findBoundCut(itemKey: BlockKey): DropAuthorization? {
        return synchronized(stateLock) {
            cleanupLocked(System.currentTimeMillis())
            cutsByWorld[itemKey.worldId]
                ?.firstOrNull { itemKey in it.markerPositions }
                ?.authorization
        }
    }

    private fun discoverAndBindCut(item: Item, itemKey: BlockKey): DropAuthorization? {
        if (!item.hasTreeCapitatorMarker()) return null

        val markerPositions = item.world
            .getNearbyEntities(item.location, SESSION_RANGE, SESSION_RANGE, SESSION_RANGE)
            .asSequence()
            .filter { it.isTreeCapitatorMarker() }
            .map { it.location.toMagneticBlockKey() }
            .toSet()
        if (markerPositions.isEmpty()) return null

        return synchronized(stateLock) {
            val now = System.currentTimeMillis()
            cleanupLocked(now)

            cutsByWorld[itemKey.worldId]
                ?.firstOrNull { itemKey in it.markerPositions }
                ?.authorization
                ?: bindCutLocked(itemKey.worldId, markerPositions, now)
        }
    }

    private fun bindCutLocked(
        worldId: UUID,
        markerPositions: Set<BlockKey>,
        now: Long
    ): DropAuthorization? {
        val sessions = sessionsByWorld[worldId] ?: return null
        val session = sessions
            .asSequence()
            .map { candidate ->
                candidate to markerPositions.minOf { candidate.root.distanceSquared(it) }
            }
            .filter { (_, distance) -> distance <= SESSION_RANGE * SESSION_RANGE }
            .minWithOrNull(
                compareBy<Pair<Session, Double>> { (_, distance) -> distance }
                    .thenByDescending { (candidate) -> candidate.id }
            )
            ?.first
            ?: return null

        sessions.remove(session)
        if (sessions.isEmpty()) sessionsByWorld.remove(worldId)
        cutsByWorld.getOrPut(worldId, ::mutableListOf)
            .add(Cut(session.authorization, markerPositions, now + SESSION_TTL_MS))
        return session.authorization
    }

    private fun Item.hasTreeCapitatorMarker(): Boolean {
        return getNearbyEntities(MARKER_RANGE, MARKER_RANGE, MARKER_RANGE)
            .any { it.isTreeCapitatorMarker() }
    }

    private fun org.bukkit.entity.Entity.isTreeCapitatorMarker(): Boolean {
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

    private fun cleanupLocked(now: Long) {
        sessionsByWorld.entries.removeIf { (_, sessions) ->
            sessions.removeIf { it.expiresAt <= now }
            sessions.isEmpty()
        }
        cutsByWorld.entries.removeIf { (_, cuts) ->
            cuts.removeIf { it.expiresAt <= now }
            cuts.isEmpty()
        }
    }

    private fun Location.toMagneticBlockKey(): BlockKey {
        return BlockKey(world.uid, blockX, blockY, blockZ)
    }
}
