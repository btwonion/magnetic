rootProject.name = "magnetic"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"
    shared {
        version("26.1.2-fabric", "26.1.2")
        version("26.1.2-neoforge", "26.1.2")
        version("26.2-fabric", "26.2")
        version("26.2-neoforge", "26.2")
        vcsVersion = "26.2-fabric"
    }
    create("mod")
}

include(":paper")
