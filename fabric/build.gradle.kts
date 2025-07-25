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

    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    // Use CurseMaven for Serilum mods as long as Modrinth doesn't fix the description bug https://github.com/modrinth/code/issues/3152
    exclusiveContent {
        forRepository {
            maven("https://cursemaven.com")
        }
        filter {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.layered {
        val quiltMappings: String = property("deps.quiltmappings").toString()
        if (quiltMappings.isNotEmpty()) mappings("org.quiltmc:quilt-mappings:$quiltMappings:intermediary-v2")
        officialMojangMappings()
    })

    implementation("org.vineflower:vineflower:1.11.1")
    modImplementation("net.fabricmc:fabric-loader:0.16.14")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fapi")!!}")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.3+kotlin.2.1.21")

    modImplementation("dev.isxander:yet-another-config-lib:${property("deps.yacl")!!}")
    modCompileOnly("com.terraformersmc:modmenu:${property("deps.modMenu")!!}")

    // Compatibility mods
    modCompileOnly("maven.modrinth:rightclickharvest:9jOYB5rp")
    modCompileOnly("maven.modrinth:veinminer:n6Nt0h4H")
    modCompileOnly("maven.modrinth:fallingtree:hB7NfdzA")
    modCompileOnly("curse.maven:tree-harvester-367178:6355493")
    modCompileOnly("curse.maven:collective-342584:6390780")

    include(implementation("dev.nyon:konfig:3.0.0")!!)
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

    curseforge {
        projectId = "1244695"
        accessToken = providers.environmentVariable("CURSEFORGE_API_KEY")
        minecraftVersions.addAll(supportedMcVersions.map {
            val split = it.split('-')
            return@map if (split.size > 1) "${split[0]}-Snapshot"
            else it
        }.toSet())

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