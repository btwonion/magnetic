@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("xyz.jpenilla.resource-factory-paper-convention")

    id("me.modmuss50.mod-publish-plugin")

    `maven-publish`
}

val beta: Int? = property("beta").toString().toIntOrNull() // The pattern is '1.0.0-beta1-1.20.6-pre.2'
val featureVersion = "${property("featureVersion")}${if (beta != null) "-beta$beta" else ""}"
val mcVersion = property("mcVersion")!!.toString()
val mcVersionName = property("versionName")!!.toString()
version = "$featureVersion-$mcVersionName+paper"

group = "dev.nyon"
val githubRepo = "btwonion/magnetic"

base {
    archivesName.set(rootProject.name)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.nyon.dev/releases")
    maven("https://nexus.neetgames.com/repository/maven-public") // mcMMO
    maven("https://maven.enginehub.org/repo/") // dependency of mcMMO
    maven("https://central.sonatype.com/repository/maven-snapshots") // AuraSkills
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.foliaDevBundle("$mcVersion-R0.1-SNAPSHOT")

    implementation("dev.nyon:konfig:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    compileOnly("de.miraculixx:veinminer:2.4.7")
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.050-SNAPSHOT")
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.5")
}

val modId = property("modId").toString()
paperPluginYaml {
    name = modId

    main = "dev.nyon.magnetic.Main"
    bootstrapper = "dev.nyon.magnetic.MagneticBootstrapper"
    loader = "dev.nyon.magnetic.MagneticLoader"

    foliaSupported = true
    apiVersion = "1.21"

    dependencies.server("Veinminer", PaperPluginYaml.Load.BEFORE, required = false, joinClasspath = true)
    dependencies.server("mcMMO", PaperPluginYaml.Load.BEFORE, required = false, joinClasspath = true)
    dependencies.server("AuraSkills", PaperPluginYaml.Load.BEFORE, required = false, joinClasspath = true)
}

tasks {
    register("releasePlugin") {
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

runPaper.folia.registerTask()

val changelogText = buildString {
    append("# v${project.version}\n")
    rootProject.file("changelog.md").readText().also(::append)
}

val supportedMcVersions: List<String> =
    property("supportedMcVersions")!!.toString().split(',').map(String::trim).filter(String::isNotEmpty)
publishMods {
    displayName = "v${project.version}"
    changelog = changelogText
    file = tasks.jar.get().archiveFile
    type = if (beta != null) BETA else STABLE
    modLoaders.addAll("paper", "folia", "purpur")

    modrinth {
        projectId = "LLfA8jAD"
        accessToken = providers.environmentVariable("MODRINTH_API_KEY")
        minecraftVersions.addAll(supportedMcVersions)
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