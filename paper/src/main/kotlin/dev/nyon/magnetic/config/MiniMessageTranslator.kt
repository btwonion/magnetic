package dev.nyon.magnetic.config

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator
import org.bukkit.Bukkit
import java.util.*
import kotlin.io.path.*

object MiniMessageTranslator : MiniMessageTranslator() {
    private val supportedLanguages = listOf("en_us", "de_de")
    private val translationEntries = mutableMapOf<String, MutableMap<String, String>>()

    private val translationsDir = Bukkit.getPluginsFolder().resolve("magnetic/translations/").toPath().createDirectories()

    fun loadTranslations() {
        // Create all language files in the plugins folder or add missing keys
        supportedLanguages.forEach { language ->
            val inputStream =
                this::class.java.classLoader.getResourceAsStream("assets/translations/$language.properties") ?: return@forEach
            val original = inputStream.bufferedReader().use { it.readLines() }
            val destination = translationsDir.resolve("$language.properties")
            if (destination.notExists()) destination.createFile()

            // Check which keys still have to be added
            val destinationText = destination.readLines()
            original.toMutableList().filter { originalLine ->
                if (originalLine.isEmpty()) return@filter false
                val key = originalLine.split('=').first()
                destinationText.none { it.contains(key) }
            }.also(destination::appendLines)
        }

        // Add all the configured language keys to local cache
        val entries = translationsDir.listDirectoryEntries()
        entries.forEach { path ->
            val language = path.nameWithoutExtension
            path.readLines().forEach { line ->
                val (key, value) = line.split('=')
                if (translationEntries[key] == null) translationEntries[key] = mutableMapOf(language to value)
                else translationEntries[key]?.put(language, value)
            }
        }
    }

    override fun getMiniMessageString(key: String, locale: Locale): String? {
        val values = translationEntries[key] ?: return null
        val language = locale.toString().lowercase()
        return values[language] ?: return values["en_us"]
    }

    override fun name(): Key {
        return Key.key("magnetic:translator")
    }
}