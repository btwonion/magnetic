# Multiloader Mod Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the Fabric mod subproject into a Stonecutter and ModStitch multiloader project with an active Minecraft `26.2-pre-4` Fabric/Quilt node and staged NeoForge source support, without changing the Paper subproject.

**Architecture:** Rename `fabric/` to `mod/`, make `:mod` a Stonecutter controller, and compile shared preprocessed sources through `:mod:26.2-fabric`. Keep loader-specific lifecycle code behind Stonecutter directives while leaving mixins and gameplay behavior shared; do not register a NeoForge node until its dependency properties are supplied.

**Tech Stack:** Gradle Kotlin DSL, Stonecutter 0.9, ModStitch 0.8.4, Fabric Loom, Fabric API, Kotlin 2.3.10, Minecraft `26.2-pre-4`, Mod Publish Plugin.

## Global Constraints

- Keep Minecraft `26.2-pre-4` as the only active mod compilation target.
- Publish the active mod artifact for Fabric and Quilt; stage source and metadata for NeoForge.
- Do not select, invent, or require NeoForge, KotlinLangForge, or NeoForge YACL version properties.
- Do not register a NeoForge Stonecutter node in this change.
- Do not modify any file under `paper/`.
- Preserve existing Fabric gameplay, configuration, mixin, datagen, and publishing behavior.
- Repository instructions prohibit the agent from running Gradle build or test commands; provide those commands to the user instead.
- Use `apply_patch` for authored edits and preserve unrelated user changes.

---

## File Map

- `settings.gradle.kts`: includes unchanged Paper and defines the Stonecutter tree rooted at `:mod`.
- `build.gradle.kts`: keeps root release orchestration and redirects Fabric references to the active mod node.
- `gradle/libs.versions.toml`: centralizes mod build plugin and library versions.
- `mod/stonecutter.gradle.kts`: controller tasks and active version selection.
- `mod/build.gradle.kts`: shared ModStitch build, platform dependency selection, packaging, publishing, and class-tweaker conversion.
- `mod/versions/26.2-fabric/gradle.properties`: active Minecraft and Fabric dependency matrix.
- `mod/src/main/templates/fabric.mod.json`: generated Fabric metadata.
- `mod/src/main/templates/META-INF/neoforge.mods.toml`: dormant future NeoForge metadata.
- `mod/src/main/kotlin/dev/nyon/magnetic/Main.kt`: loader-selected entrypoint, config location, command registration, tick registration, and config-screen registration.
- `mod/src/main/kotlin/dev/nyon/magnetic/Animation.kt`: shared animation state plus a loader-neutral `tick()` function.
- `mod/src/main/kotlin/dev/nyon/magnetic/DropEvent.kt`: loader-neutral drop handler.
- `mod/src/main/kotlin/dev/nyon/magnetic/datagen/MagneticIds.kt`: shared enchantment identifiers used by runtime and datagen code.
- `mod/src/main/kotlin/dev/nyon/magnetic/datagen/{DataGenerator,EnchantmentProvider,TagProvider}.kt`: Fabric-only datagen implementations.
- `mod/src/main/kotlin/dev/nyon/magnetic/config/screen/ModMenuImpl.kt`: Fabric-only Mod Menu adapter.
- `mod/src/main/java/dev/nyon/magnetic/mixins/{ServerLevelMixin,RegistryLoadTaskMixin}.java`: call the shared handler and identifier holder.
- `mod/src/main/kotlin/dev/nyon/magnetic/utils/MixinHelper.kt`: calls the shared drop handler.
- `CLAUDE.md`: documents the new mod layout and loader boundary.

---

### Task 1: Create the Stonecutter Project Layout

**Files:**
- Move: `fabric/` to `mod/`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts:19-20,32-40,51`
- Create: `gradle/libs.versions.toml`
- Create: `mod/stonecutter.gradle.kts`
- Move/Replace: `mod/gradle.properties` with `mod/versions/26.2-fabric/gradle.properties`

**Interfaces:**
- Produces: Gradle projects `:mod`, `:mod:26.2-fabric`, and `:paper`.
- Produces: active-node properties `vers.versionName`, `vers.mcVersion`, `vers.mcVersionRange`, `vers.supportedMcVersions`, `vers.deps.fapi`, `vers.deps.yacl`, and `vers.deps.modMenu`.
- Consumes: existing root properties `beta`, `featureVersion`, `modId`, and `description` without changing Paper's property contract.

- [ ] **Step 1: Confirm the new layout does not already exist**

Run:

```bash
test -d mod && test -f mod/stonecutter.gradle.kts && test -f mod/versions/26.2-fabric/gradle.properties
```

Expected: exit status `1`, because the migration has not been applied.

- [ ] **Step 2: Move the Fabric tree and version properties**

Use the mechanical moves:

```bash
git mv fabric mod
mkdir -p mod/versions/26.2-fabric
git mv mod/gradle.properties mod/versions/26.2-fabric/gradle.properties
```

Replace `mod/versions/26.2-fabric/gradle.properties` with:

```properties
vers.versionName=26.2
vers.mcVersion=26.2-pre-4
vers.mcVersionRange=>26.1
vers.supportedMcVersions=26.2-pre-4

vers.deps.fapi=0.150.3+26.2
vers.deps.yacl=3.9.4+26.2-fabric
vers.deps.modMenu=20.0.0-alpha.1

modstitch.platform=fabric-loom
```

- [ ] **Step 3: Define the root Stonecutter tree**

Replace `settings.gradle.kts` with:

```kotlin
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
        version("26.2-fabric", "26.2-pre-4")
        vcsVersion = "26.2-fabric"
    }
    create("mod")
}

include(":paper")
```

- [ ] **Step 4: Add the version catalog and controller**

Create `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.3.10"
mod-publish = "1.1.+"
fabric-loader = "0.18.4"
fabric-language-kotlin = "1.13.9+kotlin."
konfig = "3.0.1"
modstitch = "0.8.4"

[libraries]
konfig = { module = "dev.nyon:konfig", version.ref = "konfig" }

[plugins]
kotlin = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
mod-publish = { id = "me.modmuss50.mod-publish-plugin", version.ref = "mod-publish" }
modstitch = { id = "dev.isxander.modstitch.base", version.ref = "modstitch" }
```

Create `mod/stonecutter.gradle.kts`:

```kotlin
plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric" /* [SC] DO NOT EDIT */
```

- [ ] **Step 5: Redirect root orchestration to the active node**

In `build.gradle.kts`:

- remove `id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false`;
- change the supported-version provider to:

```kotlin
val supportedMcVersions: List<String> =
    project(":mod:26.2-fabric").property("vers.supportedMcVersions").toString()
        .split(',').map(String::trim).filter(String::isNotEmpty)
```

- change the release dependency to:

```kotlin
dependsOn(":mod:26.2-fabric:releaseMod")
```

- change the webhook icon path to:

```kotlin
avatarUrl = "https://raw.githubusercontent.com/btwonion/magnetic/master/mod/src/main/resources/assets/magnetic/icon.png"
```

- [ ] **Step 6: Verify the structural contract without invoking Gradle**

Run:

```bash
test -d mod/src/main
test -f mod/stonecutter.gradle.kts
test -f mod/versions/26.2-fabric/gradle.properties
test ! -e fabric
rg -n 'version\("26\.2-fabric", "26\.2-pre-4"\)|create\("mod"\)|include\(":paper"\)' settings.gradle.kts
rg -n ':mod:26\.2-fabric:releaseMod|project\(":mod:26\.2-fabric"\)' build.gradle.kts
```

Expected: every `test` exits `0`; `rg` prints each new project reference once.

- [ ] **Step 7: Commit the layout**

```bash
git add settings.gradle.kts build.gradle.kts gradle/libs.versions.toml mod
git commit -m "build: create Stonecutter mod project"
```

---

### Task 2: Replace Loom Build Logic with ModStitch

**Files:**
- Replace: `mod/build.gradle.kts`
- Delete: `mod/src/main/resources/fabric.mod.json`
- Create: `mod/src/main/templates/fabric.mod.json`
- Create: `mod/src/main/templates/META-INF/neoforge.mods.toml`

**Interfaces:**
- Consumes: Task 1's active-node `vers.*` properties.
- Produces: `modstitch.finalJarTask`, `releaseMod`, the `maven` publication, generated loader metadata, and Stonecutter constants `fabric` and `neoforge`.
- Produces: metadata replacements `github`, `icon`, `mc`, `fabric_loader`, `flk`, `fapi`, `yacl`, and `modmenu`.

- [ ] **Step 1: Confirm the old Loom-only build is still present**

Run:

```bash
rg -n 'net\.fabricmc\.fabric-loom|minecraft\("com\.mojang:minecraft|filesMatching\("fabric\.mod\.json"\)' mod/build.gradle.kts
```

Expected: matches for the old Loom plugin, direct Minecraft dependency, and manual metadata expansion.

- [ ] **Step 2: Replace the plugin, version, metadata, and platform setup**

Replace the top-level plugin and ModStitch configuration in `mod/build.gradle.kts` with:

```kotlin
@file:Suppress("SpellCheckingInspection", "UnstableApiUsage", "RedundantNullableReturnType")

import net.fabricmc.loom.api.fabricapi.FabricApiExtension
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
        }
    }
}
```

Add the shared repositories from the existing build plus NeoForge:

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com")
    maven("https://repo.nyon.dev/releases")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.nucleoid.xyz")
    maven("https://maven.neoforged.net/releases/")
}
```

- [ ] **Step 3: Add platform-aware dependencies**

Use these helpers and declarations in `mod/build.gradle.kts`:

```kotlin
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
```

Do not add `vers.deps.fml`, `vers.deps.klf`, or a NeoForge YACL value to the active Fabric property file.

- [ ] **Step 4: Port tasks, publishing, and Java configuration**

Retain the current publishing destinations and credentials, but use the final ModStitch jar:

```kotlin
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

fun <T> prop(property: String, block: (String) -> T?): T? =
    (System.getenv(property) ?: findProperty(property)?.toString())
        ?.takeUnless(String::isBlank)
        ?.let(block)
```

- [ ] **Step 5: Replace concrete Fabric metadata with templates**

Delete `mod/src/main/resources/fabric.mod.json` and create `mod/src/main/templates/fabric.mod.json`:

```json
{
    "schemaVersion": 1,
    "id": "${mod_id}",
    "version": "${mod_version}",
    "name": "${mod_name}",
    "description": "${mod_description}",
    "contact": {
        "issues": "https://github.com/${github}/issues",
        "sources": "https://github.com/${github}"
    },
    "authors": ["${mod_author}"],
    "icon": "${icon}",
    "license": ["${mod_license}"],
    "environment": "*",
    "depends": {
        "fabricloader": ">=${fabric_loader}",
        "fabric-language-kotlin": ">=${flk}",
        "fabric-api": ">=${fapi}",
        "yet_another_config_lib_v3": ">=${yacl}",
        "minecraft": "${mc}"
    },
    "suggests": {
        "modmenu": "${modmenu}"
    },
    "entrypoints": {
        "fabric-datagen": ["dev.nyon.magnetic.datagen.DataGenerator"],
        "main": [{
            "adapter": "kotlin",
            "value": "dev.nyon.magnetic.MainKt::init"
        }],
        "modmenu": [{
            "adapter": "kotlin",
            "value": "dev.nyon.magnetic.config.screen.ModMenuImpl"
        }]
    },
    "custom": {
        "modmenu": {
            "links": {
                "modmenu.discord": "https://discord.gg/pmHTtZnMd3"
            }
        }
    }
}
```

Create `mod/src/main/templates/META-INF/neoforge.mods.toml`:

```toml
modLoader = "klf"
loaderVersion = "[1,)"
license = "${mod_license}"
issueTrackerURL = "https://github.com/${github}/issues"

[[mods]]
modId = "${mod_id}"
version = "${mod_version}"
displayName = "${mod_name}"
authors = "${mod_author}"
description = '''
${mod_description}
'''
logoFile = "${icon}"

[[dependencies.${mod_id}]]
modId = "minecraft"
versionRange = "${mc}"
ordering = "NONE"
side = "BOTH"
```

- [ ] **Step 6: Verify ModStitch ownership statically**

Run:

```bash
rg -n 'alias\(libs\.plugins\.modstitch\)|minecraftVersion = mcVersion|classTweaker\.set|configs\.register\("magnetic"\)' mod/build.gradle.kts
rg -n 'modstitch\.platform=fabric-loom' mod/versions/26.2-fabric/gradle.properties
test ! -f mod/src/main/resources/fabric.mod.json
test -f mod/src/main/templates/fabric.mod.json
test -f mod/src/main/templates/META-INF/neoforge.mods.toml
```

Expected: all required ModStitch declarations print and every `test` exits `0`.

- [ ] **Step 7: Commit the shared build**

```bash
git add mod/build.gradle.kts mod/src/main/resources/fabric.mod.json mod/src/main/templates
git commit -m "build: configure shared ModStitch packaging"
```

---

### Task 3: Isolate Loader Lifecycle Hooks

**Files:**
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/Main.kt`
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/Animation.kt`
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/config/screen/ModMenuImpl.kt`

**Interfaces:**
- Produces: `fun init()` for Fabric metadata.
- Produces: dormant `object MagneticEntrypoint` annotated with `@Mod("magnetic")` for NeoForge.
- Produces: `internal fun Animation.tick()` called by loader tick hooks.
- Consumes: existing `ConfigCommand.registerCommand(CommandDispatcher<CommandSourceStack>)` and `generateConfigScreen(Screen?): Screen`.

- [ ] **Step 1: Prove Fabric lifecycle imports are currently in shared logic**

Run:

```bash
rg -n '^import net\.fabricmc' mod/src/main/kotlin/dev/nyon/magnetic/Main.kt mod/src/main/kotlin/dev/nyon/magnetic/Animation.kt
```

Expected: Fabric command, loader, and server-tick imports are reported.

- [ ] **Step 2: Convert `Main.kt` to loader-selected initialization**

Replace `Main.kt` with:

```kotlin
@file:Suppress("unused")

package dev.nyon.magnetic

import dev.nyon.konfig.config.config
import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.ConfigCommand
import dev.nyon.magnetic.config.migrate
import java.nio.file.Path

/*? if fabric {*/
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.Commands

fun init() {
    initialize(FabricLoader.getInstance().configDir.resolve("magnetic.json"))
    CommandRegistrationCallback.EVENT.register { dispatcher, _, environment ->
        if (environment != Commands.CommandSelection.DEDICATED) return@register
        ConfigCommand.registerCommand(dispatcher)
    }
    ServerTickEvents.END_LEVEL_TICK.register { Animation.tick() }
}
/*?} else if neoforge {*//*
import dev.nyon.magnetic.config.screen.generateConfigScreen
import net.minecraft.commands.Commands
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent

@Mod("magnetic")
object MagneticEntrypoint {
    private val loader = FMLLoader.getCurrent()

    init {
        initialize(loader.gameDir.resolve("config/magnetic.json"))
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { event ->
            if (event.commandSelection != Commands.CommandSelection.DEDICATED) return@addListener
            ConfigCommand.registerCommand(event.dispatcher)
        }
        NeoForge.EVENT_BUS.addListener<LevelTickEvent.Post> {
            Animation.tick()
        }

        if (loader.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
                IConfigScreenFactory { _, parent -> generateConfigScreen(parent) }
            }
        }
    }
}
*//*?}*/

private fun initialize(configPath: Path) {
    config(configPath, 5, Config()) { _, element, version ->
        migrate(element, version)
    }
}
```

The NeoForge branch is intentionally uncompiled until a NeoForge node is registered; keep its APIs isolated inside the Stonecutter branch.

- [ ] **Step 3: Make animation ticking loader-neutral**

Remove the `ServerTickEvents` import and replace the `tickListener` property in `Animation.kt` with:

```kotlin
internal fun tick() {
    animationScope.launch {
        val copiedItemEntities: Map<ItemEntity, ServerPlayer>
        trackedItemEntitiesMutex.withLock {
            copiedItemEntities = trackedItemEntities.toMap()
        }

        copiedItemEntities.forEach { (itemEntity, target) ->
            val targetPos = target.position()
            val itemEntityPos = itemEntity.position()
            val vec = targetPos.subtract(itemEntityPos)
            val length = vec.length()
            val tickPart = blocksPerTick / length
            val tickVec = vec.multiply(
                tickPart,
                if (itemEntity.horizontalCollision) tickPart * 2 else tickPart,
                tickPart
            )
            itemEntity.addDeltaMovement(tickVec)
        }
    }
}
```

Do not alter `pullItemToPlayer` or `invokePickupItemEntity`.

- [ ] **Step 4: Make the Mod Menu adapter Fabric-only**

Wrap the imports and class in `ModMenuImpl.kt`:

```kotlin
package dev.nyon.magnetic.config.screen

/*? if fabric {*/
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

@Suppress("unused")
class ModMenuImpl : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory(::generateConfigScreen)
}
/*?}*/
```

- [ ] **Step 5: Verify loader imports are bounded**

Run:

```bash
rg -n '/\*\? if fabric|else if neoforge|@Mod\("magnetic"\)|internal fun tick' \
  mod/src/main/kotlin/dev/nyon/magnetic/Main.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/Animation.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/config/screen/ModMenuImpl.kt
! rg -n '^import net\.fabricmc' mod/src/main/kotlin/dev/nyon/magnetic/Animation.kt
```

Expected: both loader branches and `Animation.tick()` print; the negative Fabric import check exits `0`.

- [ ] **Step 6: Commit the lifecycle boundary**

```bash
git add mod/src/main/kotlin/dev/nyon/magnetic/Main.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/Animation.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/config/screen/ModMenuImpl.kt
git commit -m "refactor: isolate mod loader lifecycle hooks"
```

---

### Task 4: Remove Fabric API from Shared Drop and Datagen Logic

**Files:**
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/DropEvent.kt`
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/utils/MixinHelper.kt`
- Modify: `mod/src/main/java/dev/nyon/magnetic/mixins/ServerLevelMixin.java`
- Create: `mod/src/main/kotlin/dev/nyon/magnetic/datagen/MagneticIds.kt`
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/datagen/DataGenerator.kt`
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/datagen/EnchantmentProvider.kt`
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/datagen/TagProvider.kt`
- Modify: `mod/src/main/java/dev/nyon/magnetic/mixins/RegistryLoadTaskMixin.java`

**Interfaces:**
- Produces: `operator fun DropEvent.invoke(items: MutableList<ItemStack>, exp: MutableInt, player: ServerPlayer, pos: BlockPos)`.
- Produces: shared top-level values `magneticEffectId: TagKey<Enchantment>` and `magneticEnchantmentId: Identifier` in `MagneticIds.kt`.
- Consumes: `Animation.pullItemToPlayer`, config condition evaluation, inventory alerts, and existing mixin calls.

- [ ] **Step 1: Confirm the shared code still depends on Fabric Event API**

Run:

```bash
rg -n 'EventFactory|Event<DropEventConsumer>|\.event\.invoker\(\)|getEvent\(\)' \
  mod/src/main/kotlin/dev/nyon/magnetic/DropEvent.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/utils/MixinHelper.kt \
  mod/src/main/java/dev/nyon/magnetic/mixins/ServerLevelMixin.java
```

Expected: the Fabric event factory and both invocation paths print.

- [ ] **Step 2: Replace the event wrapper with a direct handler**

Replace `DropEvent.kt` with:

```kotlin
package dev.nyon.magnetic

import dev.nyon.magnetic.config.Config
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.centerVec
import dev.nyon.magnetic.mixins.ExperienceOrbInvoker
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.mutable.MutableInt
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object DropEvent {
    @Suppress("KotlinConstantConditions")
    operator fun invoke(
        items: MutableList<ItemStack>,
        exp: MutableInt,
        player: ServerPlayer,
        pos: BlockPos
    ) {
        if (!config.conditionStatement.checkAndReport(player)) return

        if (config.itemsAllowed) {
            items.removeIf { item ->
                if (config.animation.enabled && canAddItem(item, player)) {
                    Animation.pullItemToPlayer(item, pos.centerVec(), player)
                    return@removeIf true
                }
                if (item.isEmpty) return@removeIf true
                val copiedStack = item.copy()
                if (!player.addItem(item)) {
                    tickInventoryAlert(player)
                    return@removeIf false
                }
                player.awardStat(Stats.ITEM_PICKED_UP.get(copiedStack.item), copiedStack.count)
                true
            }
        }

        if (config.expAllowed) {
            val fakeExperienceOrb = ExperienceOrb(player.level(), 0.0, 0.0, 0.0, exp.toInt())
            player.take(fakeExperienceOrb, 1)
            val leftExp = (fakeExperienceOrb as ExperienceOrbInvoker)
                .invokeRepairPlayerItems(player, exp.toInt())
            if (leftExp > 0) player.giveExperiencePoints(leftExp)
            exp.value = 0
        }
    }

    private val cooldowns: Map<Config.FullInventoryAlert.Alert, MutableMap<UUID, Instant>> = mapOf(
        config.fullInventoryAlert.soundAlert to mutableMapOf(),
        config.fullInventoryAlert.textAlert to mutableMapOf(),
        config.fullInventoryAlert.titleAlert to mutableMapOf()
    )

    private fun tickInventoryAlert(player: ServerPlayer) {
        val currentTime = Clock.System.now()
        cooldowns.forEach { (alert, playerCooldowns) ->
            if (!alert.enabled) return@forEach
            val lastAlert = playerCooldowns[player.uuid]
            if (lastAlert == null || currentTime > lastAlert + alert.cooldownInSeconds.seconds) {
                playerCooldowns[player.uuid] = currentTime
                alert.invoke(player)
            }
        }
    }

    private fun canAddItem(stack: ItemStack, player: Player): Boolean {
        if (player.inventory.freeSlot >= 0) return true
        if (player.hasInfiniteMaterials()) return true
        if (stack.isDamaged) return false
        return player.inventory.getSlotWithRemainingSpace(stack) > -1
    }
}
```

Delete `DropEventConsumer`; no subscriber API remains.

- [ ] **Step 3: Update both invocation sites**

In `MixinHelper.modifyExpressionValuePlayerExp`, use:

```kotlin
val mutableInt = MutableInt(exp)
DropEvent(ArrayList(), mutableInt, player, pos)
return mutableInt.toInt()
```

In `ServerLevelMixin.interceptEntity`, use:

```java
DropEvent.INSTANCE.invoke(
    items,
    new MutableInt(0),
    player,
    entity.blockPosition()
);
```

- [ ] **Step 4: Split shared identifiers from Fabric datagen**

Create `MagneticIds.kt`:

```kotlin
package dev.nyon.magnetic.datagen

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.enchantment.Enchantment

val magneticEffectId: TagKey<Enchantment> =
    TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("magnetic", "auto_move"))
val magneticEnchantmentId: Identifier =
    Identifier.fromNamespaceAndPath("magnetic", "magnetic")
```

Remove those declarations and their now-unused Minecraft imports from `DataGenerator.kt`. Change `RegistryLoadTaskMixin.java` to:

```java
import dev.nyon.magnetic.datagen.MagneticIdsKt;
```

and:

```java
.equals(MagneticIdsKt.getMagneticEnchantmentId())) ci.cancel();
```

- [ ] **Step 5: Guard Fabric datagen implementations**

Replace `DataGenerator.kt` with:

```kotlin
package dev.nyon.magnetic.datagen

/*? if fabric {*/
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class DataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()
        pack.addProvider(::EnchantmentProvider)
        pack.addProvider(::EnchantmentTagProvider)
    }
}
/*?}*/
```

Replace `EnchantmentProvider.kt` with:

```kotlin
package dev.nyon.magnetic.datagen

/*? if fabric {*/
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition
import net.minecraft.world.item.enchantment.Enchantment.dynamicCost
import java.util.concurrent.CompletableFuture

class EnchantmentProvider(
    output: FabricPackOutput,
    registriesFuture: CompletableFuture<HolderLookup.Provider>
) : FabricDynamicRegistryProvider(output, registriesFuture) {
    override fun getName(): String = "Magnetic Enchantment Generation"

    override fun configure(registries: HolderLookup.Provider, entries: Entries) {
        val enchantmentDefinition: EnchantmentDefinition = Enchantment.definition(
            registries.lookupOrThrow(Registries.ITEM).getOrThrow(ConventionalItemTags.TOOLS),
            2,
            1,
            dynamicCost(25, 25),
            dynamicCost(75, 75),
            7,
            EquipmentSlotGroup.HAND
        )
        val enchantment = Enchantment.enchantment(enchantmentDefinition).build(
            Identifier.fromNamespaceAndPath("magnetic", "magnetic.name")
        )
        entries.add(ResourceKey.create(Registries.ENCHANTMENT, magneticEnchantmentId), enchantment)
    }
}
/*?}*/
```

Replace `TagProvider.kt` with:

```kotlin
package dev.nyon.magnetic.datagen

/*? if fabric {*/
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.EnchantmentTags
import net.minecraft.world.item.enchantment.Enchantment
import java.util.concurrent.CompletableFuture

class EnchantmentTagProvider(
    output: FabricPackOutput,
    completableFuture: CompletableFuture<HolderLookup.Provider>
) : FabricTagsProvider<Enchantment>(output, Registries.ENCHANTMENT, completableFuture) {
    override fun addTags(registries: HolderLookup.Provider) {
        val enchantmentResourceKey = ResourceKey.create(Registries.ENCHANTMENT, magneticEnchantmentId)
        listOf(
            builder(magneticEffectId),
            builder(EnchantmentTags.TRADEABLE),
            builder(EnchantmentTags.IN_ENCHANTING_TABLE),
            builder(EnchantmentTags.TREASURE)
        ).forEach { it.addOptional(enchantmentResourceKey) }
    }
}
/*?}*/
```

- [ ] **Step 6: Verify no unguarded Fabric API remains in shared gameplay code**

Run:

```bash
! rg -n 'EventFactory|Event<DropEventConsumer>|DropEventConsumer|\.event\.invoker\(\)|getEvent\(\)' mod/src/main
rg -n 'DropEvent\(|DropEvent\.INSTANCE\.invoke' \
  mod/src/main/kotlin/dev/nyon/magnetic/utils/MixinHelper.kt \
  mod/src/main/java/dev/nyon/magnetic/mixins/ServerLevelMixin.java
rg -n '/\*\? if fabric' mod/src/main/kotlin/dev/nyon/magnetic/datagen/{DataGenerator,EnchantmentProvider,TagProvider}.kt
rg -n 'magneticEffectId|magneticEnchantmentId' mod/src/main/kotlin/dev/nyon/magnetic/datagen/MagneticIds.kt
```

Expected: the negative check exits `0`; both direct handler calls, all three datagen guards, and both shared identifiers print.

- [ ] **Step 7: Commit the shared logic boundary**

```bash
git add mod/src/main/kotlin/dev/nyon/magnetic/DropEvent.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/utils/MixinHelper.kt \
  mod/src/main/java/dev/nyon/magnetic/mixins/ServerLevelMixin.java \
  mod/src/main/java/dev/nyon/magnetic/mixins/RegistryLoadTaskMixin.java \
  mod/src/main/kotlin/dev/nyon/magnetic/datagen
git commit -m "refactor: make mod gameplay logic loader neutral"
```

---

### Task 5: Document and Audit the Migration

**Files:**
- Modify: `CLAUDE.md`
- Verify only: all changed files

**Interfaces:**
- Consumes: Tasks 1-4.
- Produces: accurate repository guidance and a user-run verification handoff.

- [ ] **Step 1: Update repository guidance**

In `CLAUDE.md`:

- change the project overview to Fabric/Quilt + staged NeoForge + Paper;
- replace the `fabric/` tree entry with this `mod/` structure:

```text
mod/                         # Stonecutter + ModStitch multiloader mod
  src/main/java/             # Shared Java mixins
  src/main/kotlin/           # Shared/preprocessed Kotlin logic
  src/main/resources/        # Shared runtime resources and class tweaker
  src/main/templates/        # Fabric and NeoForge metadata templates
  versions/26.2-fabric/      # Active Fabric version node
paper/                       # Independent Paper plugin
```

- explain that loader branches use Stonecutter directives and NeoForge is not an active node yet;
- update every key Fabric file path from `fabric/src/...` to `mod/src/...`;
- keep the instruction that the user, not the agent, runs builds and in-game tests.

- [ ] **Step 2: Audit Paper isolation**

Run:

```bash
git diff 6a3d52f -- paper
```

Expected: no output.

- [ ] **Step 3: Audit the project and source invariants**

Run:

```bash
git diff --check
test ! -e fabric
test -f mod/versions/26.2-fabric/gradle.properties
test ! -d mod/versions/26.2-neoforge
rg -n '26\.2-pre-4|modstitch\.platform=fabric-loom' mod/versions/26.2-fabric/gradle.properties
rg -n 'version\("26\.2-fabric"|vcsVersion = "26\.2-fabric"' settings.gradle.kts
rg -n 'fabric|neoforge' mod/build.gradle.kts mod/src/main/kotlin/dev/nyon/magnetic/Main.kt
```

Expected: no whitespace errors; only the Fabric version directory exists; the active version and both loader branches print.

- [ ] **Step 4: Ask the user to run Gradle verification**

Provide these exact commands without running them as the agent:

```bash
./gradlew projects
./gradlew :mod:26.2-fabric:build
./gradlew :paper:build
```

Expected:

- `projects` lists `:mod`, `:mod:26.2-fabric`, and `:paper`, with no NeoForge node;
- the Fabric build completes and emits a `magnetic-3.12.1-26.2+fabric.jar`-style artifact under `mod/versions/26.2-fabric/build/libs/`;
- the Paper build continues to complete without Paper source or build changes.

- [ ] **Step 5: Give the user the Fabric runtime checklist**

Ask the user to verify in a Fabric client/server environment:

```text
1. Magnetic initializes and creates config/magnetic.json.
2. /magnetic reload succeeds on a dedicated server.
3. Mod Menu opens the Magnetic YACL screen and saves changes.
4. Magnetic items and XP enter the player inventory.
5. Animated items move to the player and are picked up.
6. The magnetic enchantment and its tags load from generated data.
```

- [ ] **Step 6: Commit documentation**

```bash
git add CLAUDE.md
git commit -m "docs: describe multiloader mod layout"
```

- [ ] **Step 7: Report the deferred NeoForge activation contract**

State explicitly in the handoff:

```text
NeoForge source and metadata are staged but not built. To activate it later, add a
Stonecutter `26.2-neoforge` node plus `mod/versions/26.2-neoforge/gradle.properties`
containing `vers.mcVersion`, `vers.mcVersionRange`, `vers.supportedMcVersions`,
`vers.deps.fml`, `vers.deps.klf`, `vers.deps.yacl`, and
`modstitch.platform=moddevgradle`, then compile and correct the dormant API branch
against those selected versions.
```
