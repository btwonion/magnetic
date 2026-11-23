package dev.nyon.magnetic.compat

import dev.nyon.magnetic.Animation
import dev.nyon.magnetic.DropEvent
import dev.nyon.magnetic.DropEventDispatcher
import dev.nyon.magnetic.extensions.isIgnored
import dev.nyon.magnetic.extensions.listen
import org.apache.commons.lang3.mutable.MutableInt
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Item
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.ItemSpawnEvent

/**
 * Collects items and experience that other plugins spawn themselves while a player's [BlockBreakEvent] is handled.
 *
 * Auto-smelt plugins commonly disable the vanilla drops and spawn the smelted result directly, so neither
 * `BlockDropItemEvent` nor Veinminer's drop event ever sees it. Veinminer fires a [BlockBreakEvent] subclass for
 * every block it breaks, so this covers vein-mined blocks as well.
 */
object BlockBreakDropCapture {
    private class Capture(val event: BlockBreakEvent) {
        val items = mutableListOf<Item>()
        val orbs = mutableListOf<ExperienceOrb>()
    }

    // Break events can nest, for example when a plugin breaks further blocks from its own listener.
    // Each region thread handles its own breaks on Folia, so the stack is thread-local.
    private val captures = ThreadLocal.withInitial { ArrayDeque<Capture>() }

    fun listenForEvents() {
        listen<BlockBreakEvent>(EventPriority.LOWEST) {
            captures.get().addLast(Capture(this))
        }

        listen<ItemSpawnEvent>(EventPriority.MONITOR) {
            if (isCancelled) return@listen
            currentCapture()?.items?.add(entity)
        }

        listen<EntitySpawnEvent>(EventPriority.MONITOR) {
            if (isCancelled) return@listen
            val orb = entity as? ExperienceOrb ?: return@listen
            currentCapture()?.orbs?.add(orb)
        }

        // MONITOR handlers run even when the event was cancelled, so the capture is always closed here.
        listen<BlockBreakEvent>(EventPriority.MONITOR) {
            val stack = captures.get()
            val index = stack.indexOfLast { it.event === this }
            if (index < 0) return@listen
            val capture = stack.removeAt(index)
            if (stack.isEmpty()) captures.remove()

            if (!isCancelled) collect(capture)
        }
    }

    private fun currentCapture(): Capture? = captures.get().lastOrNull()

    private fun collect(capture: Capture) {
        val event = capture.event
        val block = event.block
        val items = capture.items.filter {
            it.isValid && it.world == block.world && !Animation.tracksItem(it) && !SmeltingDatapackCompat.isDeferred(it)
        }
        val orbs = capture.orbs.filter { it.isValid && it.world == block.world }
        if (items.isEmpty() && orbs.isEmpty()) return
        if (block.type.isIgnored) return

        val player = event.player
        val authorization = DropEventDispatcher.authorize(player) ?: return

        items.forEach { item ->
            // Auto-smelt datapacks still need to smelt the plugin's items.
            if (SmeltingDatapackCompat.defersDrops(player)) {
                SmeltingDatapackCompat.collectAfterDatapack(item, authorization)
                return@forEach
            }

            val stacks = mutableListOf(item.itemStack.clone())
            DropEventDispatcher.callAuthorized(
                DropEvent(stacks, MutableInt(), player, item.location),
                authorization
            )

            val remaining = stacks.firstOrNull()
            if (remaining == null || remaining.isEmpty) item.remove() else item.itemStack = remaining
        }

        val experience = orbs.sumOf { it.experience }
        if (experience <= 0) return
        val remainingExperience = MutableInt(experience)
        DropEventDispatcher.callAuthorized(
            DropEvent(mutableListOf(), remainingExperience, player, block.location),
            authorization
        )
        if (remainingExperience.toInt() == 0) orbs.forEach(ExperienceOrb::remove)
    }
}
