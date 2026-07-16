@file:Suppress("SpellCheckingInspection", "UnstableApiUsage", "RedundantNullableReturnType")

import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.modstitch)
    alias(libs.plugins.mod.publish)
    `maven-publish`
}

val isFabric = modstitch.isLoom
val loader = if (isFabric) "fabric" else "neoforge"
val beta: Int? = property("beta").toString().toIntOrNull()
val featureVersion = "${property("featureVersion")}${if (beta != null) "-beta$beta" else ""}"
val mcVersion = property("vers.mcVersion").toString()
val mcVersionName = property("vers.versionName").toString()
val generatedResources =
    rootProject.layout.projectDirectory.dir("mod/generated/$mcVersionName")
version = "$featureVersion-$mcVersionName+$loader"
group = "dev.nyon"

val githubRepo = "btwonion/magnetic"
val fabricLoader = libs.versions.fabric.loader.get()
val fabricLanguageKotlin = "${libs.versions.fabric.language.kotlin.get()}${libs.versions.kotlin.get()}"

base {
    archivesName.set(rootProject.name)
}

modstitch {
    minecraftVersion = mcVersion
    classTweaker.set(rootProject.layout.projectDirectory.file("mod/src/main/resources/magnetic.classtweaker"))

    metadata {
        modId = property("modId").toString()
        modName = property("modId").toString()
        modDescription = property("description").toString()
        modGroup = project.group.toString()
        modVersion = project.version.toString()
        modLicense = "GNU General Public License v3.0"
        modAuthor = "btwonion"

        replacementProperties.put("github", githubRepo)
        replacementProperties.put("icon", "assets/magnetic/icon.png")
        replacementProperties.put("mc", property("vers.mcVersionRange").toString())
        replacementProperties.put("fabric_loader", fabricLoader)
        replacementProperties.put("flk", fabricLanguageKotlin)
        prop("vers.deps.fapi") { replacementProperties.put("fapi", it) }
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
    }

    tasks.named<ProcessResources>("generateModMetadata") {
        dependsOn("stonecutterGenerate")
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

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com")
    maven("https://repo.nyon.dev/releases")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.neoforged.net/releases/")
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

    if (isFabric) {
        propModDependency("fapi", { "net.fabricmc.fabric-api:fabric-api:$it" }, api = true)
        modDependency("net.fabricmc:fabric-language-kotlin:$fabricLanguageKotlin")
        propModDependency("modMenu", { "com.terraformersmc:modmenu:$it" })
    } else {
        propModDependency(
            "klf",
            { "dev.nyon:KotlinLangForge:2.11.2-k${libs.versions.kotlin.get()}-$it+neoforge" },
            api = true
        )
    }

    propModDependency("yacl", { "dev.isxander:yet-another-config-lib:$it" })
    modstitchApi(libs.konfig)
    modstitchJiJ(libs.konfig)
}

tasks {
    register("releaseMod") {
        group = "publishing"
        dependsOn("publishMods")
        dependsOn("publish")
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = modstitch.javaVersion.map { JvmTarget.fromTarget(it.toString()) }
        }
        dependsOn("stonecutterGenerate")
    }
}

val changelogText = buildString {
    append("# v${project.version}\n")
    rootProject.file("changelog.md").readText().also(::append)
}

val supportedMcVersions = property("vers.supportedMcVersions").toString()
    .split(',').map(String::trim).filter(String::isNotEmpty)

publishMods {
    displayName = "v${project.version}"
    file = modstitch.finalJarTask.flatMap { it.archiveFile }
    changelog = changelogText
    type = if (beta != null) BETA else STABLE
    if (isFabric) modLoaders.addAll("fabric", "quilt") else modLoaders.add("neoforge")

    modrinth {
        projectId = "LLfA8jAD"
        accessToken = providers.environmentVariable("MODRINTH_API_KEY")
        minecraftVersions.addAll(supportedMcVersions)
        if (isFabric) {
            requires { slug = "fabric-api" }
            requires { slug = "fabric-language-kotlin" }
            optional { slug = "modmenu" }
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
        if (isFabric) {
            requires { slug = "fabric-api" }
            requires { slug = "fabric-language-kotlin" }
            optional { slug = "modmenu" }
        } else {
            requires { slug = "kotlinlangforge" }
        }
        requires { slug = "yacl" }
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
