@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import net.fabricmc.loom.configuration.FabricApiExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("fabric-loom") version "1.9-SNAPSHOT"

    id("me.modmuss50.mod-publish-plugin") version "0.8.+"

    `maven-publish`
}

val beta: Int? = null // Pattern is '1.0.0-beta1-1.20.6-pre.2'
val featureVersion = "3.3.0${if (beta != null) "-beta$beta" else ""}"
val mcVersion = property("mcVersion")!!.toString()
val mcVersionRange = property("mcVersionRange")!!.toString()
val mcVersionName = property("versionName")!!.toString()
version = "$featureVersion-$mcVersionName"

group = "dev.nyon"
val githubRepo = "btwonion/magnetic"

base {
    archivesName.set(rootProject.name)
}

loom {
    accessWidenerPath = rootDir.resolve("src/main/resources/magnetic.accesswidener")
    mixin { useLegacyMixinAp = false }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com")
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://repo.nyon.dev/releases")
    maven("https://maven.isxander.dev/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.layered {
        val quiltMappings: String = property("deps.quiltmappings").toString()
        if (quiltMappings.isNotEmpty()) mappings("org.quiltmc:quilt-mappings:$quiltMappings:intermediary-v2")
        officialMojangMappings()
    })

    implementation("org.vineflower:vineflower:1.10.1")
    modImplementation("net.fabricmc:fabric-loader:0.16.9")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fapi")!!}")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.0+kotlin.2.1.0")

    modImplementation("dev.isxander:yet-another-config-lib:${property("deps.yacl")!!}")
    modImplementation("com.terraformersmc:modmenu:${property("deps.modMenu")!!}")

    include(modImplementation("dev.nyon:konfig:2.0.2-1.20.4")!!)
}

tasks {
    processResources {
        val modId = "magnetic"
        val modName = "magnetic"
        val modDescription = "Magnetically moves items and experience into your inventory. Also known as telekinesis from Hypixel Skyblock."

        val props = mapOf(
            "id" to modId,
            "name" to modName,
            "description" to modDescription,
            "version" to project.version,
            "github" to githubRepo,
            "mc" to mcVersionRange
        )

        props.forEach(inputs::property)

        filesMatching("fabric.mod.json") {
            expand(props)
        }
    }

    register("releaseMod") {
        group = "publishing"

        dependsOn("publishMods")
        dependsOn("publish")
    }

    register("postUpdate") {
        group = "publishing"

        val url = providers.environmentVariable("DISCORD_WEBHOOK").orNull ?: return@register
        val roleId = providers.environmentVariable("DISCORD_ROLE_ID").orNull ?: return@register
        val changelogText = rootProject.file("changelog.md").readText()
        val webhook = DiscordWebhook(
            username = "${rootProject.name} Release Notifier",
            avatarUrl = "https://raw.githubusercontent.com/btwonion/magnetic/master/src/main/resources/assets/magnetic/icon.png",
            embeds = listOf(
                Embed(
                    title = "v$featureVersion of ${rootProject.name} released!",
                    description = "# Changelog\n$changelogText",
                    timestamp = Instant.now().toString(),
                    color = 0xff0080,
                    fields = listOf(
                        Field(
                            "Supported versions",
                            property("supportedMcVersions")!!.toString().split(',').joinToString(),
                            false
                        ),
                        Field("Modrinth", "https://modrinth.com/mod/magnetic", true),
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

    withType<JavaCompile> {
        options.release = 21
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("21")
        }
    }
}

val changelogText = buildString {
    append("# v${project.version}\n")
    rootProject.file("changelog.md").readText().also(::append)
}

val supportedMcVersions: List<String> =
    property("supportedMcVersions")!!.toString().split(',').map(String::trim).filter(String::isNotEmpty)

publishMods {
    displayName = "v${project.version}"
    file = tasks.remapJar.get().archiveFile
    changelog = changelogText
    type = if (beta != null) BETA else STABLE
    modLoaders.addAll("fabric", "quilt")

    modrinth {
        projectId = "LLfA8jAD"
        accessToken = providers.environmentVariable("MODRINTH_API_KEY")
        minecraftVersions.addAll(supportedMcVersions)

        requires { slug = "fabric-api" }
        requires { slug = "fabric-language-kotlin" }
        requires { slug = "yacl" }
        optional { slug = "modmenu" }
    }

    github {
        repository = githubRepo
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        commitish = "master"
    }
}

publishing {
    repositories {
        maven {
            name = "nyon"
            url = uri("https://repo.nyon.dev/releases")
            credentials {
                username = providers.environmentVariable("NYON_USERNAME").orNull
                password = providers.environmentVariable("NYON_PASSWORD").orNull
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.nyon"
            artifactId = "magnetic"
            version = project.version.toString()
            from(components["java"])
        }
    }
}

java {
    withSourcesJar()

    JavaVersion.VERSION_21.let {
        sourceCompatibility = it
        targetCompatibility = it
    }
}

data class Field(val name: String, val value: String, val inline: Boolean)

data class Embed(
    val title: String, val description: String, val timestamp: String, val color: Int, val fields: List<Field>
)

data class DiscordWebhook(
    val username: String, val avatarUrl: String, val embeds: List<Embed>
)