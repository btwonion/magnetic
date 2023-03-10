package dev.nyon.telekinesis

import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_19_R2.CraftServer
import org.bukkit.enchantments.Enchantment
import org.bukkit.plugin.java.JavaPlugin
import kotlin.io.path.createFile
import kotlin.io.path.exists

val bukkitEnchantment: Enchantment = BukkitTelekinesis()

class Main : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: Main; private set
    }

    override fun onLoad() {
        INSTANCE = this
        configPath = Bukkit.getPluginsFolder().toPath().resolve("telekinesis.toml")
            .also { if (!it.exists()) it.createFile() }
        Telekinesis.init()
        if (dev.nyon.telekinesis.config.enchantment) {
            addToBukkitRegistry()
            addToMinecraftRegistry()
        }
    }

    override fun onEnable() {
        initBlockListeners()
        initMobListeners()
    }
}

private fun addToBukkitRegistry() {
    val field = Enchantment::class.java.getDeclaredField("acceptingNew")
    field.isAccessible = true
    field.set(null, true)
    Enchantment.registerEnchantment(BukkitTelekinesis())
    field.set(null, false)
    field.isAccessible = true
}

@Suppress("unchecked_cast")
private fun addToMinecraftRegistry() {
    val server = (Bukkit.getServer() as CraftServer).handle.server
    val enchantmentRegistry = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT) as MappedRegistry
    val enchantmentRegistryClass = enchantmentRegistry.javaClass
    val frozenField = enchantmentRegistryClass.getDeclaredField("l") // l - frozen
    frozenField.isAccessible = true
    frozenField.set(enchantmentRegistry, false)

    Registry.register(
        enchantmentRegistry,
        ResourceLocation("telekinesis", "telekinesis"),
        telekinesis
    )

    frozenField.set(enchantmentRegistry, true)
    frozenField.isAccessible = false

    val byValueField = enchantmentRegistryClass.getDeclaredField("h") // h - byValue
    byValueField.isAccessible = true
    val byValueMap =
        byValueField.get(enchantmentRegistry) as Map<*, Holder.Reference<*>>
    val value = byValueMap[telekinesis]!!
    val method = value.javaClass.getDeclaredMethod("b", Any::class.java)
    method.isAccessible = true
    method.invoke(value, telekinesis) // b - bindValue
    method.isAccessible = false
    byValueField.isAccessible = false
}

val Plugin by lazy { Main.INSTANCE }