package dev.nyon.magnetic.config.conditions

import dev.nyon.magnetic.common.CooldownTracker
import dev.nyon.magnetic.common.conditions.ConditionChain
import dev.nyon.magnetic.common.conditions.ConditionType
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.datagen.magneticEffectId
/*? if >=1.21.11 {*/
import dev.nyon.magnetic.extensions.PlayerPermissionSupplier
//? if neoforge
//import dev.nyon.magnetic.extensions.hasNeoForgePermission
/*?} else {*/
/*import dev.nyon.magnetic.extensions.hasMagneticPermission
*//*?}*/
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
/*? if >=1.21.11 {*/
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionCheck
/*?}*/
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("magnetic")
private val errorReports = CooldownTracker<UUID>()
private const val ERROR_REPORT_COOLDOWN_MILLIS = 60_000L

//? if >=1.21.11
private val permission = Permission.Atom.create("magnetic.ability.use")

fun ConditionChain.checkAndReport(player: ServerPlayer): Boolean = try {
    evaluate { it.check(player) }
} catch (e: IllegalStateException) {
    if (errorReports.tryAcquire(player.uuid, ERROR_REPORT_COOLDOWN_MILLIS)) {
        logger.error("Magnetic's condition statement '{}' can't be evaluated", raw, e)
        player.sendSystemMessage(
            Component.literal("[magnetic] There seems to be an error with the condition statements. Please contact the server administrator to check the logs for more information.")
                .withStyle(ChatFormatting.RED)
        )
    }
    false
}

private fun ConditionType.check(player: ServerPlayer): Boolean = when (this) {
    ConditionType.ENCHANTMENT -> listOf(player.mainHandItem, player.offhandItem).any {
        EnchantmentHelper.hasTag(it, magneticEffectId) || config.buckets.enabled && it.`is`(Items.BUCKET)
    }
    ConditionType.SNEAK -> player.isCrouching
    ConditionType.PERMISSION -> hasAbilityPermission(player)
}

private fun hasAbilityPermission(player: ServerPlayer): Boolean {
    var result: Boolean
    //? if >=1.21.11
    result = Commands.hasPermission<PlayerPermissionSupplier>(PermissionCheck.Require(permission)).test(PlayerPermissionSupplier(player))
    //? if >=1.21.11 && neoforge
    //result = result || hasNeoForgePermission(player)
    //? if <1.21.11
    //result = player.hasMagneticPermission()

    return result
}
