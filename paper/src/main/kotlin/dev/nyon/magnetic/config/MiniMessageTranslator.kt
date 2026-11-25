package dev.nyon.magnetic.config

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import java.nio.file.Path
import java.util.Locale
import java.util.Properties
import kotlin.io.path.*
//? if >=1.21.6 {
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator
//?} else {
/*import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.translation.Translator
import java.text.MessageFormat
*///?}

//? if >=1.21.6
object MiniMessageTranslator : MiniMessageTranslator() {
//? if <1.21.6
//object MiniMessageTranslator : Translator {
    private val supportedLanguages = listOf("en_us", "de_de")
    private val translations = mutableMapOf<String, Map<String, String>>()

    private val translationsDir = Bukkit.getPluginsFolder().resolve("magnetic/translations/").toPath().createDirectories()

    fun loadTranslations() {
        supportedLanguages.forEach { language ->
            val bundled = this::class.java.classLoader.getResourceAsStream("assets/translations/$language.properties")
                ?.use { it.readBytes().decodeToString() } ?: return@forEach
            val destination = translationsDir.resolve("$language.properties")
            if (destination.notExists()) destination.createFile()

            val existingKeys = readProperties(destination).stringPropertyNames()
            val missingLines = bundled.lines().filter { line ->
                line.isNotBlank() && line.substringBefore('=').trim() !in existingKeys
            }
            if (missingLines.isEmpty()) return@forEach
            val separator = if (destination.readText().let { it.isNotEmpty() && !it.endsWith('\n') }) "\n" else ""
            destination.appendText(missingLines.joinToString("\n", prefix = separator, postfix = "\n"))
        }

        translationsDir.listDirectoryEntries("*.properties").forEach { path ->
            val properties = readProperties(path)
            translations[path.nameWithoutExtension] = properties.stringPropertyNames().associateWith(properties::getProperty)
        }
    }

    private fun readProperties(path: Path) = Properties().apply { path.bufferedReader().use { load(it) } }

    private fun miniMessageString(key: String, locale: Locale): String? =
        translations[locale.toString().lowercase()]?.get(key) ?: translations["en_us"]?.get(key)

    //? if >=1.21.6 {
    override fun getMiniMessageString(key: String, locale: Locale): String? = miniMessageString(key, locale)
    //?} else {
    /*override fun translate(key: String, locale: Locale): MessageFormat? = null

    override fun translate(component: TranslatableComponent, locale: Locale): Component? {
        val text = miniMessageString(component.key(), locale) ?: return null
        return MiniMessage.miniMessage().deserialize(text).applyFallbackStyle(component.style())
    }
    *///?}

    override fun name(): Key {
        return Key.key("magnetic:translator")
    }
}