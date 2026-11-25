import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    }
}

repositories {
    mavenCentral()
}

val modTargets = project(":mod").subprojects
val paperTargets = project(":paper").subprojects

tasks {
    register("testFast") {
        group = "verification"
        description = "Run JVM tests for the shared module and every mod target"
        dependsOn(":common:test")
        dependsOn(modTargets.map { "${it.path}:test" })
    }

    register("testGameLatest") {
        group = "verification"
        description = "Run headless gameplay tests on the latest Fabric and NeoForge targets"
        dependsOn(":mod:26.3-fabric:runGameTest")
        dependsOn(":mod:26.3-neoforge:runGameTest")
    }

    register("testGamePaper") {
        group = "verification"
        description = "Run headless Paper and Folia gameplay tests for every Paper target"
        dependsOn(paperTargets.map { "${it.path}:runGameTest" })
    }

    register("testAll") {
        group = "verification"
        description = "Build every platform and run all automated test layers"
        dependsOn(":common:build")
        dependsOn((modTargets + paperTargets).map { "${it.path}:build" })
        dependsOn("testGameLatest", "testGamePaper")
    }

    register("releaseAllPlatforms") {
        group = "publishing"

        dependsOn((modTargets + paperTargets).map { "${it.path}:release" })
    }

    register("postUpdate") {
        group = "publishing"
        description = "Announce the release in Discord"

        val webhookUrl = providers.environmentVariable("DISCORD_WEBHOOK")
        val roleId = providers.environmentVariable("DISCORD_ROLE_ID")
        val changelog = rootProject.file("changelog.md")
        val title = "v$featureVersion of ${rootProject.name} released!"
        val notifierName = "${rootProject.name} Release Notifier"
        val modVersions = modTargets.flatMap { it.supportedMcVersions }.distinct().joinToString()
        val paperVersions = paperTargets.flatMap { it.supportedMcVersions }.distinct().joinToString()
        onlyIf("DISCORD_WEBHOOK and DISCORD_ROLE_ID are set") { webhookUrl.isPresent && roleId.isPresent }

        doLast {
            fun field(name: String, value: String, inline: Boolean) = buildJsonObject {
                put("name", name)
                put("value", value)
                put("inline", inline)
            }

            val embed = buildJsonObject {
                put("title", title)
                put("description", "# Changelog\n${changelog.readText()}")
                put("timestamp", Instant.now().toString())
                put("color", 0xff0080)
                put("fields", JsonArray(listOf(
                    field("Fabric & NeoForge versions", modVersions, false),
                    field("Paper versions", paperVersions, false),
                    field("Supported loaders", "Fabric, Quilt, NeoForge, Paper, Folia, PurPur", false),
                    field("Modrinth", "https://modrinth.com/mod/magnetic", true),
                    field("CurseForge", "https://www.curseforge.com/minecraft/mc-mods/magnetic-telekinesis", true),
                    field("GitHub", "https://github.com/btwonion/magnetic", true)
                )))
            }
            val message = buildJsonObject {
                put("username", notifierName)
                put("avatar_url", "https://raw.githubusercontent.com/btwonion/magnetic/master/mod/src/main/resources/assets/magnetic/icon.png")
                put("content", "<@&${roleId.get()}>")
                put("embeds", JsonArray(listOf(embed)))
            }

            HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(webhookUrl.get()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(message.toString()))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
        }
    }
}
