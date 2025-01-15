@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("net.minecrell.plugin-yml.bukkit")

    id("me.modmuss50.mod-publish-plugin")

    `maven-publish`
}

val beta: Int? = property("beta").toString().toIntOrNull() // Pattern is '1.0.0-beta1-1.20.6-pre.2'
val featureVersion = "${property("featureVersion")}${if (beta != null) "-beta$beta" else ""}"
val mcVersion = property("mcVersion")!!.toString()
val mcVersionName = property("versionName")!!.toString()
version = "$featureVersion-$mcVersionName-paper"

group = "dev.nyon"
val githubRepo = "btwonion/magnetic"

base {
    archivesName.set(rootProject.name)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.nyon.dev/releases")
}

dependencies {
    paperweight.paperDevBundle("$mcVersion-R0.1-SNAPSHOT")

    library(kotlin("stdlib"))

    implementation("dev.nyon:konfig:2.0.2-1.20.4")
}

tasks {
    register("releasePlugin") {
        group = "publishing"

        dependsOn("publishMods")
        dependsOn("publish")
    }

    assemble {
        dependsOn(reobfJar)
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
    file(tasks.jar.get().archiveFile)
    changelog = changelogText
    type = if (beta != null) BETA else STABLE
    modLoaders.addAll("paper", "folia", "purpur")

    modrinth {
        projectId = "LLfA8jAD"
        accessToken = providers.environmentVariable("MODRINTH_API_KEY")
        minecraftVersions.addAll(
            listOf(
                "1.17",
                "1.17.1",
                "1.18",
                "1.18.1",
                "1.18.2",
                "1.19",
                "1.19.1",
                "1.19.2",
                "1.19.3",
                "1.19.4",
                "1.20",
                "1.20.1",
                "1.20.2",
                "1.20.3",
                "1.20.4",
                "1.20.5",
                "1.20.6",
                "1.21",
                "1.21.1",
                "1.21.2",
                "1.21.3",
                "1.21.4"
            )
        )
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