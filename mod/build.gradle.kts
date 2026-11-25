@file:Suppress("SpellCheckingInspection", "UnstableApiUsage", "RedundantNullableReturnType", "AvoidDuplicateDependencies")

import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import dev.isxander.modstitch.base.moddevgradle.BaseModDevGradleExtension
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.slf4j.event.Level

buildscript {
    // Modstitch depends on an older mod-publish plugin; use only the one buildSrc provides.
    configurations.classpath {
        exclude(group = "me.modmuss50", module = "mod-publish-plugin")
    }
}

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.moddev) apply false
    alias(libs.plugins.modstitch)
    id("me.modmuss50.mod-publish-plugin")
    `maven-publish`
}

val isFabric = modstitch.isLoom
val loader = if (isFabric) "fabric" else "neoforge"
val mcVersion = property("vers.mcVersion").toString()
val mcVersionName = property("vers.versionName").toString()
val generatedResources =
    rootProject.layout.projectDirectory.dir("mod/generated/$mcVersionName")
useMagneticVersion(loader)

val fabricLoader = libs.versions.fabric.loader.get()
val fabricLanguageKotlin = "${libs.versions.fabric.language.kotlin.get()}${libs.versions.kotlin.get()}"

base {
    archivesName.set(rootProject.name)
}

modstitch {
    minecraftVersion = mcVersion
    classTweaker.set(rootProject.layout.projectDirectory.file("mod/src/main/resources/magnetic.classtweaker"))
    if (isFabric) unitTesting()

    metadata {
        modId = property("modId").toString()
        modName = property("modId").toString()
        modDescription = property("description").toString()
        modGroup = project.group.toString()
        modVersion = project.version.toString()
        modLicense = "GNU General Public License v3.0"
        modAuthor = "btwonion"

        replacementProperties.put("github", Magnetic.GITHUB_REPO)
        replacementProperties.put("icon", "assets/magnetic/icon.png")
        replacementProperties.put("icon_key", if (stonecutter.eval(mcVersion, ">=26.3")) "iconFile" else "logoFile")
        replacementProperties.put("mc", property("vers.mcVersionRange").toString())
        replacementProperties.put("fabric_loader", fabricLoader)
        replacementProperties.put("flk", fabricLanguageKotlin)
        (prop("vers.deps.fapiMin") { it } ?: prop("vers.deps.fapi") { it })?.let { replacementProperties.put("fapi", it) }
        prop("vers.deps.yacl") { replacementProperties.put("yacl", it) }
        prop("vers.deps.modMenu") { replacementProperties.put("modmenu", it) }
    }

    loom {
        fabricLoaderVersion = fabricLoader
        configureLoom {
            runConfigs.all {
                ideConfigGenerated(false)
            }
        }
    }

    moddevgradle {
        prop("vers.deps.fml") { neoForgeVersion = it }
    }

    mixin {
        addMixinsToModManifest = true
        configs.register("magnetic")
        configs.register("magnetic.compat")
    }
}

stonecutter {
    constants["fabric"] = isFabric
    constants["neoforge"] = !isFabric
}

if (isFabric) {
    extensions.configure<FabricApiExtension> {
        configureDataGeneration {
            client = true
            outputDirectory = generatedResources.asFile
        }
        if (mcVersionName == "26.3") {
            configureTests {
                createSourceSet = true
                modId = "magnetic_test"
                enableGameTests = true
                enableClientGameTests = false
                eula = true
            }
        }
    }

    if (mcVersionName == "26.3") {
        extensions.getByType<SourceSetContainer>().named("gametest") {
            java.setSrcDirs(listOf(
                rootProject.file("mod/src/gametestCommon/java"),
                rootProject.file("mod/src/gametestFabric/java")
            ))
            resources.setSrcDirs(listOf(
                rootProject.file("mod/src/gametestCommon/resources"),
                rootProject.file("mod/src/gametestFabric/resources")
            ))
        }

        // Loom normally attaches runGameTest to test. Keep the fast JVM layer
        // separate; the root testGameLatest/testAll tasks invoke it explicitly.
        tasks.named<Test>("test") {
            setDependsOn(dependsOn.filterNot {
                it == "runGameTest" ||
                    (it as? Task)?.name == "runGameTest" ||
                    (it as? TaskProvider<*>)?.name == "runGameTest"
            })
        }
    }

    tasks.named<ProcessResources>("generateModMetadata") {
        dependsOn("stonecutterGenerate")
    }
}

if (!isFabric && mcVersionName == "26.3") {
    val sourceSets = extensions.getByType<SourceSetContainer>()
    val gameTest = sourceSets.create("gameTest") {
        java.setSrcDirs(listOf(
            rootProject.file("mod/src/gametestCommon/java"),
            rootProject.file("mod/src/gametestNeoForge/java")
        ))
        resources.setSrcDirs(listOf(
            rootProject.file("mod/src/gametestCommon/resources"),
            rootProject.file("mod/src/gametestNeoForge/resources")
        ))
        compileClasspath += sourceSets.named("main").get().output
        runtimeClasspath += output + compileClasspath
    }

    configurations.named(gameTest.implementationConfigurationName) {
        extendsFrom(configurations.named("implementation").get())
    }
    configurations.named(gameTest.runtimeOnlyConfigurationName) {
        extendsFrom(configurations.named("runtimeOnly").get())
    }

    extensions.getByType<BaseModDevGradleExtension>().configureNeoForge {
        val testMod = mods.register("magnetic_test") {
            sourceSet(gameTest)
        }
        runs.register("gameTest") {
            type = "gameTestServer"
            sourceSet = gameTest
            loadedMods.addAll(mods.named("mod").get(), testMod.get())
            gameDirectory = layout.buildDirectory.dir("run/gameTest")
            logLevel = Level.INFO
            systemProperty("neoforge.enableGameTest", "true")
        }
    }
    afterEvaluate {
        extensions.getByType<BaseModDevGradleExtension>().configureNeoForge {
            addModdingDependenciesTo(gameTest)
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("stonecutterGenerate")
    if (isFabric) {
        dependsOn("generateModMetadata")
    } else {
        from(generatedResources) {
            exclude(".cache/**")
        }
    }
}

val bundledCommon: Configuration by configurations.creating {
    isTransitive = false
}

tasks.named<Jar>("jar") {
    dependsOn(bundledCommon)
    from({ bundledCommon.map(::zipTree) }) {
        exclude("META-INF/MANIFEST.MF")
    }
}

repositories {
    mavenCentral()
    // maven("https://maven.terraformersmc.com")
    // Replacement of terraformers as long as it is failing
    maven("https://maven.gnomecraft.net/releases")
    maven("https://repo.nyon.dev/releases")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.neoforged.net/releases/")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    fun modDependency(artifact: Any, compileOnly: Boolean = false, api: Boolean = false) {
        val configuration = when {
            compileOnly && api -> "modstitchModCompileOnlyApi"
            compileOnly -> "modstitchModCompileOnly"
            api -> "modstitchModApi"
            else -> "modstitchModImplementation"
        }
        add(configuration, artifact)
    }

    fun propModDependency(
        id: String,
        artifact: (String) -> String,
        compileOnly: Boolean = false,
        api: Boolean = false
    ) {
        prop("vers.deps.$id") { modDependency(artifact(it), compileOnly, api) }
    }

    fun propCompileOnlyDependency(id: String, artifact: (String) -> String) {
        prop("vers.deps.$id") {
            add("compileOnly", artifact(it)) {
                isTransitive = false
            }
        }
    }

    if (isFabric) {
        propModDependency("fapi", { "net.fabricmc.fabric-api:fabric-api:$it" }, api = true)
        modDependency("net.fabricmc:fabric-language-kotlin:$fabricLanguageKotlin")
        propModDependency("modMenu", { "com.terraformersmc:modmenu:$it" })
        if (stonecutter.eval(mcVersion, "<1.21.11"))
            modDependency("me.lucko:fabric-permissions-api:${if (stonecutter.eval(mcVersion, "<=1.21.1")) "0.3.1" else "0.5.0"}", api = true)
    } else {
        propModDependency(
            "klf",
            { "dev.nyon:KotlinLangForge:2.11.2-k${libs.versions.kotlin.get()}-$it+neoforge" },
            api = true
        )
    }

    propModDependency("yacl", { "dev.isxander:yet-another-config-lib:$it" })

    propCompileOnlyDependency(
        "compat.rightClickHarvest"
    ) { "maven.modrinth:rightclickharvest:$it" }
    propCompileOnlyDependency(
        "compat.veinminer"
    ) { "maven.modrinth:veinminer:$it" }
    propCompileOnlyDependency(
        "compat.fallingTree"
    ) { "maven.modrinth:fallingtree:$it" }
    propCompileOnlyDependency(
        "compat.kleeSlabs"
    ) { "maven.modrinth:kleeslabs:$it" }
    propCompileOnlyDependency(
        "compat.balm"
    ) { "maven.modrinth:balm:$it" }
    propCompileOnlyDependency(
        "compat.treeHarvester"
    ) { "maven.modrinth:tree-harvester:$it" }
    propCompileOnlyDependency(
        "compat.collective"
    ) { "maven.modrinth:collective:$it" }

    modstitchApi(libs.konfig)
    modstitchJiJ(libs.konfig)
    modstitchApi(project(":common")) { isTransitive = false }
    bundledCommon(project(":common"))

    // Keep NeoForge's pure JVM suite independent of a loader launch. The
    // oldest supported NeoForge cannot load the current KotlinLangForge in
    // ModStitch's JUnit environment; Minecraft-dependent checks are GameTests.
    if (!isFabric) {
        testImplementation(platform("org.junit:junit-bom:5.13.4"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}

if (!isFabric) {
    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = modstitch.javaVersion.map { JvmTarget.fromTarget(it.toString()) }
        }
        dependsOn("stonecutterGenerate")
    }
}

configureMagneticRelease(
    modstitch.finalJarTask.flatMap { it.archiveFile },
    if (isFabric) listOf("fabric", "quilt") else listOf("neoforge")
) {
    modrinth {
        environment = ModrinthEnvironment.CLIENT_AND_SERVER

        if (isFabric) {
            requires { slug = "fabric-api" }
            requires { slug = "fabric-language-kotlin" }
            optional { slug = "modmenu" }
            if (stonecutter.eval(mcVersion, "<1.21.11"))
                optional { slug = "fabric-permissions-api" }
        } else {
            requires { slug = "kotlin-lang-forge" }
        }
        requires { slug = "yacl" }
    }

    curseforge {
        projectId = "1244695"
        accessToken = providers.environmentVariable("CURSEFORGE_API_KEY")
        minecraftVersions.addAll(supportedMcVersions.map {
            val split = it.split('-')
            if (split.size > 1) "${split[0]}-Snapshot" else it
        }.toSet())
        client = true
        server = true

        if (isFabric) {
            requires { slug = "fabric-api" }
            requires { slug = "fabric-language-kotlin" }
            optional { slug = "modmenu" }
        } else {
            requires { slug = "kotlinlangforge" }
        }
        requires { slug = "yacl" }
    }
}

java {
    withSourcesJar()
}

if (isFabric) {
    tasks.named("sourcesJar") {
        dependsOn("generateModMetadata")
    }
}

fun <T> prop(property: String, block: (String) -> T?): T? =
    (System.getenv(property) ?: findProperty(property)?.toString())
        ?.takeUnless(String::isBlank)
        ?.let(block)
