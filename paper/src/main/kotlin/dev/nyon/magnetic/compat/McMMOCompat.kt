package dev.nyon.magnetic.compat

import com.gmail.nossr50.events.items.McMMOItemSpawnEvent
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerFishingTreasureEvent
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerShakeEvent
import dev.nyon.magnetic.extensions.listen

object McMMOCompat {
    fun listenForEvents() {
        listen<McMMOItemSpawnEvent> {
            val serverPlayer = player ?: return@listen
            if (collectDrop(itemStack, serverPlayer, location) == null) isCancelled = true
        }

        listen<McMMOPlayerFishingTreasureEvent> {
            treasure = treasure?.let { collectDrop(it, player, player.location) }
        }

        listen<McMMOPlayerShakeEvent> {
            drop = collectDrop(drop, player, player.location)
        }
    }
}
