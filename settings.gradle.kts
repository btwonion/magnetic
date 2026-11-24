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
    // Downloads a JDK when a Java toolchain is missing, such as Java 21 for the 1.21.x Paper targets.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Every Stonecutter target is registered here; each has gradle.properties in <project>/versions/<target>.
stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    // Fabric and NeoForge targets, named <Minecraft version>-<loader>.
    create("mod") {
        version("1.21.1-fabric", "1.21.1")
        version("1.21.1-neoforge", "1.21.1")
        version("1.21.11-fabric", "1.21.11")
        version("1.21.11-neoforge", "1.21.11")
        version("26.1.2-fabric", "26.1.2")
        version("26.1.2-neoforge", "26.1.2")
        version("26.2-fabric", "26.2")
        version("26.2-neoforge", "26.2")
        version("26.3-fabric", "26.3")
        version("26.3-neoforge", "26.3")
        vcsVersion = "26.3-fabric"
    }

    // Paper targets, named after the oldest release of the range each one supports.
    create("paper") {
        version("1.21.1")
        version("1.21.6")
        version("26.1")
        vcsVersion = "26.1"
    }
}
