@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runtask.service.DownloadsAPIService
import java.io.ByteArrayInputStream
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    id("me.modmuss50.mod-publish-plugin")
    alias(libs.plugins.paper.userdev)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.rfpc)

    `maven-publish`
}

val mcVersion = property("vers.mcVersion").toString()
val javaVersion = property("vers.javaVersion").toString().toInt()
useMagneticVersion("paper")

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

// Folia only publishes some patch releases, so it may need a different version than the dev bundle.
// Read on the project: inside registerTask, property() resolves against the task.
val foliaVersion = property("vers.foliaVersion").toString()
runPaper.folia.registerTask {
    minecraftVersion(foliaVersion)
}

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

val gameTestPort = property("vers.gameTestPort").toString().toInt()
val runGameTestPaper = registerGameTestServer(
    "runGameTestPaper", "Paper", property("vers.gameTestPaperVersion").toString(), gameTestPort, folia = false
)
val runGameTestFolia = registerGameTestServer("runGameTestFolia", "Folia", foliaVersion, gameTestPort + 1, folia = true)
runGameTestFolia.configure { mustRunAfter(runGameTestPaper) }

tasks.register("runGameTest") {
    group = "verification"
    description = "Run Magnetic's gameplay tests on headless Paper and Folia servers"
    dependsOn(runGameTestPaper, runGameTestFolia)
}

configureMagneticRelease(tasks.jar.flatMap { it.archiveFile }, listOf("paper", "folia", "purpur"))

java {
    withSourcesJar()

    // The toolchain also runs paperweight's setup and the test servers: the 1.21.x dev bundles'
    // remapper cannot read Java 25 classes, and 1.21.x servers run on Java 21.
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}