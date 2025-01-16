@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("fabric-loom")

    id("me.modmuss50.mod-publish-plugin")

    `maven-publish`
}

val beta: Int? = property("beta").toString().toIntOrNull() // Pattern is '1.0.0-beta1-1.20.6-pre.2'
val featureVersion = "${property("featureVersion")}${if (beta != null) "-beta$beta" else ""}"
val mcVersion = property("mcVersion")!!.toString()
val mcVersionRange = property("mcVersionRange")!!.toString()
val mcVersionName = property("versionName")!!.toString()
version = "$featureVersion-$mcVersionName+fabric"

group = "dev.nyon"
val githubRepo = "btwonion/magnetic"

base {
    archivesName.set(rootProject.name)
}

loom {
    accessWidenerPath = project.file("src/main/resources/magnetic.accesswidener")
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

val supportedMcVersions: List<String> =
    property("supportedMcVersions")!!.toString().split(',').map(String::trim).filter(String::isNotEmpty)
val modId = property("modId").toString()
val modDescription = property("description").toString()

tasks {
    processResources {
        val props = mapOf(
            "id" to modId,
            "name" to modId,
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