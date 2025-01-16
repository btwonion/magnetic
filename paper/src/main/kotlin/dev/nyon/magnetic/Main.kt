package dev.nyon.magnetic

import dev.nyon.magnetic.extensions.isEligible
import dev.nyon.magnetic.extensions.listen
import io.papermc.paper.event.block.PlayerShearBlockEvent
import org.bukkit.NamespacedKey
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import org.bukkit.plugin.java.JavaPlugin
import dev.nyon.magnetic.config as internalConfig

val magneticKey = NamespacedKey("magnetic", "magnetic")

class Main : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: Main; private set
    }

    override fun onLoad() {
        INSTANCE = this
        internalConfig
    }

    override fun onEnable() {
        listen<BlockDropItemEvent> {
            if (!internalConfig.itemsAllowed) return@listen
            if (!player.isEligible()) return@listen
            items.removeIf { player.inventory.addItem(it.itemStack).isEmpty() }
        }

        listen<BlockBreakEvent> {
            if (!internalConfig.expAllowed) return@listen
            if (!player.isEligible()) return@listen
            player.giveExp(expToDrop, true)
            expToDrop = 0
        }

        listen<EntityDeathEvent> {
            val killer = entity.killer ?: return@listen
            if (!killer.isEligible()) return@listen
            if (internalConfig.itemsAllowed) drops.removeIf { killer.inventory.addItem(it).isEmpty() }

            if (internalConfig.expAllowed) {
                killer.giveExp(droppedExp, true)
                droppedExp = 0
            }
        }

        listen<PlayerShearBlockEvent> {
            if (!internalConfig.itemsAllowed) return@listen
            if (!player.isEligible()) return@listen
            drops.removeIf { player.inventory.addItem(it).isEmpty() }
        }

        listen<PlayerShearEntityEvent> {
            if (!internalConfig.itemsAllowed) return@listen
            if (!player.isEligible()) return@listen
            drops.removeIf { player.inventory.addItem(it).isEmpty() }
        }

        listen<PlayerFishEvent> {
            if (!player.isEligible()) return@listen

            if (internalConfig.expAllowed && expToDrop != 0) {
                player.giveExp(expToDrop, true)
                expToDrop = 0
            }
        }
    }

    override fun onDisable() {
        dev.nyon.konfig.config.saveConfig(internalConfig)
    }
}

val Plugin by lazy { Main.INSTANCE }