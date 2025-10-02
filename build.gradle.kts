import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("me.modmuss50.mod-publish-plugin") version "0.8.+"

    id("fabric-loom") version "1.11-SNAPSHOT" apply false

    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17" apply false
    id("xyz.jpenilla.run-paper") version "2.3.1" apply false
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.0" apply false
}

repositories {
    mavenCentral()
}

val beta: Int? = property("beta").toString().toIntOrNull() // Pattern is '1.0.0-beta1-1.20.6-pre.2'
val featureVersion = "${property("featureVersion")}${if (beta != null) "-beta$beta" else ""}"
val supportedMcVersions: List<String> =
    project("fabric").property("supportedMcVersions")!!.toString().split(',').map(String::trim).filter(String::isNotEmpty)

tasks {
    register("releaseAllPlatforms") {
        group = "publishing"

        dependsOn(":fabric:releaseMod")
        dependsOn(":paper:releasePlugin")
    }

    register("postUpdate") {
        group = "publishing"

        val url = providers.environmentVariable("DISCORD_WEBHOOK").orNull ?: return@register
        val roleId = providers.environmentVariable("DISCORD_ROLE_ID").orNull ?: return@register
        val changelogText = rootProject.file("changelog.md").readText()
        val webhook = DiscordWebhook(
            username = "${rootProject.name} Release Notifier",
            avatarUrl = "https://raw.githubusercontent.com/btwonion/magnetic/master/fabric/src/main/resources/assets/magnetic/icon.png",
            embeds = listOf(
                Embed(
                    title = "v$featureVersion of ${rootProject.name} released!",
                    description = "# Changelog\n$changelogText",
                    timestamp = Instant.now().toString(),
                    color = 0xff0080,
                    fields = listOf(
                        Field(
                            "Supported versions", supportedMcVersions.joinToString(), false
                        ),
                        Field("Modrinth", "https://modrinth.com/mod/magnetic", true),
                        Field("CurseForge", "https://www.curseforge.com/minecraft/mc-mods/magnetic-telekinesis", true),
                        Field("GitHub", "https://github.com/btwonion/magnetic", true)
                    ),
                )
            )
        )

        @OptIn(ExperimentalSerializationApi::class)
        val embedsJson = buildJsonArray {
            webhook.embeds.map { embed ->
                add(buildJsonObject {
                    put("title", embed.title)
                    put("description", embed.description)
                    put("timestamp", embed.timestamp)
                    put("color", embed.color)
                    putJsonArray("fields") {
                        addAll(embed.fields.map { field ->
                            buildJsonObject {
                                put("name", field.name)
                                put("value", field.value)
                                put("inline", field.inline)
                            }
                        })
                    }
                })
            }
        }

        val json = buildJsonObject {
            put("username", webhook.username)
            put("avatar_url", webhook.avatarUrl)
            put("content", "<@&$roleId>")
            put("embeds", embedsJson)
        }

        val jsonString = Json.encodeToString(json)
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonString)).build(), HttpResponse.BodyHandlers.ofString()
        )
    }
}


data class Field(val name: String, val value: String, val inline: Boolean)

data class Embed(
    val title: String, val description: String, val timestamp: String, val color: Int, val fields: List<Field>
)

data class DiscordWebhook(
    val username: String, val avatarUrl: String, val embeds: List<Embed>
)