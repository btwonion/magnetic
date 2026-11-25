package dev.nyon.magnetic.config.conditions

import dev.nyon.magnetic.Main
import dev.nyon.magnetic.common.CooldownTracker
import dev.nyon.magnetic.common.conditions.ConditionChain
import dev.nyon.magnetic.common.conditions.ConditionType
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.magneticKey
import dev.nyon.magnetic.magneticPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID
import java.util.logging.Level

private val errorReports = CooldownTracker<UUID>()
private const val ERROR_REPORT_COOLDOWN_MILLIS = 60_000L

fun ConditionChain.checkAndReport(player: Player): Boolean = try {
    evaluate { it.check(player) }
} catch (e: IllegalStateException) {
    if (errorReports.tryAcquire(player.uniqueId, ERROR_REPORT_COOLDOWN_MILLIS)) {
        Main.INSTANCE.logger.log(Level.SEVERE, "Magnetic's condition statement '$raw' can't be evaluated", e)
        player.sendMessage(
            Component.text("[magnetic] There seems to be an error with the condition statements. Please contact the server administrator to check the logs for more information.")
                .color(NamedTextColor.RED)
        )
    }
    false
}

private fun ConditionType.check(player: Player): Boolean = when (this) {
    ConditionType.ENCHANTMENT -> listOf(player.inventory.itemInMainHand, player.inventory.itemInOffHand).any { item ->
        item.enchantments.keys.any { it.key == magneticKey } || config.buckets.enabled && item.type == Material.BUCKET
    }
    ConditionType.SNEAK -> player.isSneaking
    ConditionType.PERMISSION -> player.hasPermission(magneticPermission)
}
