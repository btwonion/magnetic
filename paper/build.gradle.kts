@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runtask.service.DownloadsAPIService
import java.io.ByteArrayInputStream
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.mod.publish)
    alias(libs.plugins.paper.userdev)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.rfpc)

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
    maven("https://api.modrinth.com/maven") { // Tree Feller, TimberZ
        content {
            includeGroup("maven.modrinth")
        }
    }
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle(mcVersion)

    implementation("dev.nyon:konfig:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    compileOnly("de.miraculixx:veinminer:2.4.7")
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.050-SNAPSHOT")
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.5")
    compileOnly("maven.modrinth:thizzyz-tree-feller:1.33.0")
    compileOnly("maven.modrinth:timberz:TmpFEQ0J") // 1.3.0 for 1.21.9 to 26.2
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
    dependencies.server("TreeFeller", PaperPluginYaml.Load.BEFORE, required = false, joinClasspath = true)
    dependencies.server("TimberZ", PaperPluginYaml.Load.BEFORE, required = false, joinClasspath = true)
}

tasks {
    register("releasePlugin") {
        group = "publishing"

        dependsOn("publishMods")
        dependsOn("publish")
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
        }
    }
}

// Folia only publishes some patch releases, so it may need a different version than the dev bundle.
// Read on the project: inside registerTask, property() resolves against the task.
val foliaVersion = property("foliaVersion").toString()
runPaper.folia.registerTask {
    minecraftVersion(foliaVersion)
}

// Gameplay tests: a separate test plugin, never published, that joins client-less players on a
// headless server, runs the scenarios in src/gametest and stops the server. See docs/TESTING.md.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}

val gametestJar = tasks.register<Jar>("gametestJar") {
    archiveBaseName.set("magnetic-gametest")
    destinationDirectory.set(layout.buildDirectory.dir("gametest-libs"))
    from(gametest.output)
}

fun registerGameTestServer(taskName: String, serverName: String, version: String, port: Int, folia: Boolean) =
    tasks.register<RunServer>(taskName) {
        group = "verification"
        description = "Run Magnetic's gameplay tests on a headless $serverName $version server"
        displayName.set(serverName)
        if (folia) downloadsApiService.set(DownloadsAPIService.folia(project))
        minecraftVersion(version)
        pluginJars(tasks.jar.flatMap { it.archiveFile }, gametestJar.flatMap { it.archiveFile })
        // Running these tasks accepts the Minecraft EULA for their throwaway test servers.
        systemProperty("com.mojang.eula.agree", "true")
        standardInput = ByteArrayInputStream(ByteArray(0))

        val runDir = layout.buildDirectory.dir("run/gameTest$serverName").get().asFile
        runDirectory(runDir)
        val results = runDir.resolve("magnetic-gametest-results.txt")
        doFirst {
            // Keep the downloaded libraries, but start every run from a fresh world and config.
            listOf("world", "world_nether", "world_the_end", "plugins", "logs", results.name)
                .forEach { runDir.resolve(it).deleteRecursively() }
            runDir.mkdirs()
            runDir.resolve("server.properties").writeText(
                """
                level-type=minecraft\:flat
                generator-settings={"biome"\:"minecraft\:plains","layers"\:[{"block"\:"minecraft\:bedrock","height"\:1},{"block"\:"minecraft\:stone","height"\:3}]}
                generate-structures=false
                spawn-monsters=false
                spawn-protection=0
                online-mode=false
                view-distance=4
                simulation-distance=4
                server-port=$port
                """.trimIndent()
            )
        }
        doLast {
            if (!results.exists()) throw GradleException("$serverName gameplay tests did not report results; see $runDir/logs")
            val report = results.readText()
            if (!report.lineSequence().any { it.startsWith("PASSED:") }) {
                throw GradleException("$serverName gameplay tests failed:\n$report")
            }
            logger.lifecycle(report.lines().last(String::isNotBlank).let { "$serverName gameplay tests $it" })
        }
    }

val runGameTestPaper = registerGameTestServer(
    "runGameTestPaper", "Paper", property("gameTestPaperVersion").toString(), 25581, folia = false
)
val runGameTestFolia = registerGameTestServer("runGameTestFolia", "Folia", foliaVersion, 25582, folia = true)
runGameTestFolia.configure { mustRunAfter(runGameTestPaper) }

tasks.register("testGame") {
    group = "verification"
    description = "Run Magnetic's gameplay tests on headless Paper and Folia servers"
    dependsOn(runGameTestPaper, runGameTestFolia)
}

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

    JavaVersion.VERSION_25.let {
        sourceCompatibility = it
        targetCompatibility = it
    }
}