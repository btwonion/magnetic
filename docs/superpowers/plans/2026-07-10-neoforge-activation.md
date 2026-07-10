# NeoForge 26.2 Activation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Activate and verify a Minecraft 26.2 NeoForge artifact alongside the existing Fabric/Quilt artifact without changing Paper or duplicating loader-neutral generated data.

**Architecture:** Register `26.2-neoforge` as a second Stonecutter node and let the existing ModStitch platform branches compile through ModDevGradle. Package the committed shared generated-data directory into NeoForge during resource processing, while retaining Fabric as the only data generator.

**Tech Stack:** Gradle 9.4.1 Kotlin DSL, Stonecutter 0.9, ModStitch 0.8.4, NeoForge `26.2.0.10-beta`, KotlinLangForge `3.1`, YACL `3.9.5+26.2-neoforge`, Java 25.

## Global Constraints

- Use the supplied NeoForge properties exactly; do not invent or substitute dependency versions.
- Keep `26.2-fabric` as the Stonecutter VCS-active node.
- Keep Fabric/Quilt and NeoForge both active and publishable.
- Do not modify any file under `paper/`.
- Preserve the staged deletion of `CLAUDE.md`; do not restore or commit it.
- Preserve the user's unstaged removal of `maven("https://maven.nucleoid.xyz")`; do not restore or include it in an agent commit.
- Keep Fabric as the only datagen implementation; package `mod/src/main/generated/data/**` into both loaders.
- Exclude `mod/src/main/generated/.cache/**` from NeoForge resources.
- Artifact inspection is a one-time migration check; do not add a permanent jar-validation task.
- Build all three targets. If a failure is environmental or toolchain-related, report it without repairing the environment.
- Do not execute publishing or release tasks.

---

## File Map

- `settings.gradle.kts`: registers the `26.2-neoforge` Stonecutter node.
- `mod/versions/26.2-neoforge/gradle.properties`: supplies the exact NeoForge version matrix.
- `build.gradle.kts`: aggregates supported versions and includes both loader release tasks.
- `mod/build.gradle.kts`: adds NeoForge-only shared generated resources during `processResources`.
- `mod/src/main/templates/META-INF/neoforge.mods.toml`: declares the required YACL client dependency.
- `mod/src/main/kotlin/dev/nyon/magnetic/Main.kt`: already contains the NeoForge 26.2 command, tick, config-path, and config-screen branch; modify only if compilation provides a concrete API error.

---

### Task 1: Activate the NeoForge Node and Resource Packaging

**Files:**
- Modify: `settings.gradle.kts:17-25`
- Create: `mod/versions/26.2-neoforge/gradle.properties`
- Modify: `build.gradle.kts:30-40`
- Modify: `mod/build.gradle.kts:3-5,78-86`
- Modify: `mod/src/main/templates/META-INF/neoforge.mods.toml`

**Interfaces:**
- Produces: Gradle project `:mod:26.2-neoforge`.
- Produces: exact node properties `vers.versionName`, `vers.mcVersion`, `vers.mcVersionRange`, `vers.supportedMcVersions`, `vers.deps.fml`, `vers.deps.yacl`, `vers.deps.klf`, and `modstitch.platform`.
- Produces: NeoForge `processResources` input from `mod/src/main/generated` with `.cache/**` excluded.
- Produces: root `supportedMcVersions` as a deduplicated union of both mod nodes.

- [ ] **Step 1: Record and verify the pre-existing user changes**

Run:

```bash
mkdir -p .superpowers/user-state
git diff --cached --binary -- CLAUDE.md > .superpowers/user-state/claude-deletion.patch
git diff --binary -- mod/build.gradle.kts > .superpowers/user-state/mod-build-repository-removal.patch
test -s .superpowers/user-state/claude-deletion.patch
test -s .superpowers/user-state/mod-build-repository-removal.patch
git status --short
```

Expected status includes exactly these pre-existing entries before implementation:

```text
D  CLAUDE.md
 M mod/build.gradle.kts
```

- [ ] **Step 2: Run the failing structural check**

Run:

```bash
test -d mod/versions/26.2-neoforge && \
rg -q 'version\("26\.2-neoforge", "26\.2"\)' settings.gradle.kts && \
rg -q ':mod:26\.2-neoforge:releaseMod' build.gradle.kts
```

Expected: exit status `1`, because the node does not exist yet.

- [ ] **Step 3: Register the NeoForge Stonecutter node**

Change the `shared` block in `settings.gradle.kts` to:

```kotlin
shared {
    version("26.2-fabric", "26.2-pre-4")
    version("26.2-neoforge", "26.2")
    vcsVersion = "26.2-fabric"
}
```

- [ ] **Step 4: Add the exact NeoForge properties**

Create `mod/versions/26.2-neoforge/gradle.properties`:

```properties
vers.versionName=26.2
vers.mcVersion=26.2
vers.mcVersionRange=>26.1
vers.supportedMcVersions=26.2

vers.deps.fml=26.2.0.10-beta
vers.deps.yacl=3.9.5+26.2-neoforge
vers.deps.klf=3.1

modstitch.platform=moddevgradle
```

- [ ] **Step 5: Aggregate both mod variants at the root**

Replace the current single-project supported-version declaration in root `build.gradle.kts` with:

```kotlin
val modVersionProjects = listOf(
    project(":mod:26.2-fabric"),
    project(":mod:26.2-neoforge")
)
val supportedMcVersions: List<String> = modVersionProjects
    .flatMap { modProject ->
        modProject.property("vers.supportedMcVersions").toString()
            .split(',').map(String::trim).filter(String::isNotEmpty)
    }
    .distinct()
```

Change `releaseAllPlatforms` to:

```kotlin
register("releaseAllPlatforms") {
    group = "publishing"

    dependsOn(":mod:26.2-fabric:releaseMod")
    dependsOn(":mod:26.2-neoforge:releaseMod")
    dependsOn(":paper:releasePlugin")
}
```

- [ ] **Step 6: Add shared generated data to NeoForge resources**

Add this import to `mod/build.gradle.kts`:

```kotlin
import org.gradle.language.jvm.tasks.ProcessResources
```

Immediately after the existing Fabric data-generation block, add:

```kotlin
if (!isFabric) {
    tasks.named<ProcessResources>("processResources") {
        from(rootProject.layout.projectDirectory.dir("mod/src/main/generated")) {
            exclude(".cache/**")
        }
    }
}
```

Do not re-add the Nucleoid Maven repository.

- [ ] **Step 7: Declare the NeoForge YACL runtime dependency**

Append this dependency block to `mod/src/main/templates/META-INF/neoforge.mods.toml`:

```toml

[[dependencies.${mod_id}]]
modId = "yet_another_config_lib_v3"
versionRange = "[${yacl},)"
ordering = "NONE"
side = "CLIENT"
```

The `${yacl}` replacement is already populated conditionally from `vers.deps.yacl` in `mod/build.gradle.kts`.

- [ ] **Step 8: Run the passing structural checks**

Run:

```bash
test -d mod/versions/26.2-neoforge
rg -n 'version\("26\.2-(fabric|neoforge)"|vcsVersion = "26\.2-fabric"' settings.gradle.kts
rg -n '^vers\.(versionName|mcVersion|mcVersionRange|supportedMcVersions|deps\.fml|deps\.yacl|deps\.klf)=|^modstitch\.platform=' \
  mod/versions/26.2-neoforge/gradle.properties
rg -n ':mod:26\.2-(fabric|neoforge):releaseMod' build.gradle.kts
rg -n 'ProcessResources|mod/src/main/generated|exclude\("\.cache/\*\*"\)' mod/build.gradle.kts
rg -n 'yet_another_config_lib_v3|\[\$\{yacl\},\)' mod/src/main/templates/META-INF/neoforge.mods.toml
git diff --check
test -z "$(git diff --name-only -- paper)"
```

Expected: both nodes, all exact properties, both release tasks, generated-resource inclusion, and YACL metadata print; whitespace and Paper checks exit `0`.

- [ ] **Step 9: Commit only the migration changes while preserving user state**

First remove the user's staged deletion from the index without restoring its working-tree file:

```bash
git reset -q HEAD -- CLAUDE.md
```

Stage all Task 1 files:

```bash
git add settings.gradle.kts build.gradle.kts \
  mod/versions/26.2-neoforge/gradle.properties \
  mod/build.gradle.kts \
  mod/src/main/templates/META-INF/neoforge.mods.toml
```

Remove the pre-existing Nucleoid repository deletion from the staged index while retaining it in the working tree:

```bash
git apply --cached -R .superpowers/user-state/mod-build-repository-removal.patch
```

Verify the staged diff contains the NeoForge resource change but not the user's repository removal or `CLAUDE.md` deletion:

```bash
git diff --cached --check
test -z "$(git diff --cached --name-only -- CLAUDE.md)"
git diff --cached -- mod/build.gradle.kts | rg -q 'ProcessResources'
! git diff --cached -- mod/build.gradle.kts | rg -q 'maven\.nucleoid\.xyz'
```

Commit:

```bash
git commit -m "build: activate NeoForge 26.2"
```

Restore the user's staged deletion:

```bash
git add -u CLAUDE.md
git status --short
```

Expected: the commit contains only Task 1 files, while status again includes:

```text
D  CLAUDE.md
 M mod/build.gradle.kts
```

---

### Task 2: Compile and Correct the NeoForge Source Branch

**Files:**
- Verify/Modify only if required by compiler: `mod/src/main/kotlin/dev/nyon/magnetic/Main.kt`
- Verify only: shared Kotlin and Java sources under `mod/src/main`

**Interfaces:**
- Consumes: Task 1's `:mod:26.2-neoforge` node.
- Produces: a compiling NeoForge entrypoint `MagneticEntrypoint` with command registration, client config screen, and lazy animation ticking.
- Preserves: `internal fun registerAnimationTick()` and shared `Animation.tick()` contract.

- [ ] **Step 1: Confirm the NeoForge API contract from the selected dependencies**

Before building, verify the dormant branch contains these selected NeoForge 26.2 APIs:

```bash
rg -n 'FMLLoader\.getCurrent|ModLoadingContext\.get|IConfigScreenFactory|RegisterCommandsEvent|LevelTickEvent\.Post|NeoForge\.EVENT_BUS' \
  mod/src/main/kotlin/dev/nyon/magnetic/Main.kt
```

Expected: all six API boundaries print inside the `neoforge` Stonecutter branch.

- [ ] **Step 2: Compile the NeoForge node**

Run:

```bash
./gradlew :mod:26.2-neoforge:build
```

Expected: `BUILD SUCCESSFUL` and a NeoForge jar under `mod/versions/26.2-neoforge/build/libs/`.

If the command fails because Java, Gradle, network resolution, or another environment/toolchain prerequisite is unavailable or misconfigured, capture the output and stop without changing the environment. If it fails with a source/API error, invoke `superpowers:systematic-debugging`, use the compiler's exact diagnostic as evidence, and constrain any fix to the NeoForge Stonecutter branch or loader-specific build configuration.

- [ ] **Step 3: Verify the compiled branch remains loader-isolated**

Run:

```bash
! rg -n '^import net\.neoforged' mod/src/main/kotlin \
  --glob '*.kt' \
  --glob '!**/Main.kt'
rg -n '/\*\?} else if neoforge|@Mod\("magnetic"\)|internal fun registerAnimationTick' \
  mod/src/main/kotlin/dev/nyon/magnetic/Main.kt
git diff --check
test -z "$(git diff --name-only -- paper)"
```

Expected: NeoForge imports remain confined to `Main.kt`; the branch markers and entrypoint print; whitespace and Paper checks pass.

- [ ] **Step 4: Commit a compiler-driven source correction only if one was necessary**

If Step 2 required a source or loader-build correction, commit only its exact files without including the staged `CLAUDE.md` deletion:

```bash
git commit --only mod/src/main/kotlin/dev/nyon/magnetic/Main.kt -m "fix: support NeoForge 26.2 APIs"
```

If no correction was necessary, do not create an empty commit.

---

### Task 3: Verify All Targets and Inspect Migration Artifacts Once

**Files:**
- Verify only: `mod/versions/26.2-neoforge/build/libs/*.jar`
- Verify only: `mod/versions/26.2-fabric/build/libs/*.jar`
- Verify only: Paper build output

**Interfaces:**
- Consumes: the complete dual-loader build.
- Produces: build evidence for NeoForge, Fabric, and Paper plus one-time generated-data packaging evidence.

- [ ] **Step 1: Build the Fabric and Paper targets**

Run:

```bash
./gradlew :mod:26.2-fabric:build
./gradlew :paper:build
```

Expected: both commands end with `BUILD SUCCESSFUL`.

If either failure is environmental/toolchain-related, report it without repairing the environment. Source failures introduced by this migration must be diagnosed with `superpowers:systematic-debugging`.

- [ ] **Step 2: Resolve the current non-sources mod jars**

Run:

```bash
NEOFORGE_JAR=$(find mod/versions/26.2-neoforge/build/libs -maxdepth 1 -type f \
  -name '*+neoforge.jar' ! -name '*-sources.jar' -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)
FABRIC_JAR=$(find mod/versions/26.2-fabric/build/libs -maxdepth 1 -type f \
  -name '*+fabric.jar' ! -name '*-sources.jar' -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)
test -n "$NEOFORGE_JAR" && test -f "$NEOFORGE_JAR"
test -n "$FABRIC_JAR" && test -f "$FABRIC_JAR"
printf 'NeoForge: %s\nFabric: %s\n' "$NEOFORGE_JAR" "$FABRIC_JAR"
```

Expected: one existing current jar path for each loader.

- [ ] **Step 3: Inspect generated data in both jars once**

Run:

```bash
for jar in "$NEOFORGE_JAR" "$FABRIC_JAR"; do
  listing=$(mktemp)
  jar tf "$jar" > "$listing"
  for entry in \
    data/magnetic/enchantment/magnetic.json \
    data/magnetic/tags/enchantment/auto_move.json \
    data/minecraft/tags/enchantment/in_enchanting_table.json \
    data/minecraft/tags/enchantment/tradeable.json \
    data/minecraft/tags/enchantment/treasure.json
  do
    rg -x -q "$entry" "$listing"
  done
  ! rg -q '(^|/)\.cache/' "$listing"
  rm "$listing"
done
printf 'PASS generated data packaged for Fabric and NeoForge\n'
```

Expected: the PASS message prints. Do not add this inspection as a Gradle task or repository script.

- [ ] **Step 4: Inspect NeoForge loader metadata once**

Run:

```bash
NEO_LISTING=$(mktemp)
jar tf "$NEOFORGE_JAR" > "$NEO_LISTING"
rg -x -q 'META-INF/neoforge.mods.toml' "$NEO_LISTING"
rg -x -q 'magnetic.mixins.json' "$NEO_LISTING"
rg -x -q 'META-INF/accesstransformer.cfg' "$NEO_LISTING"
rm "$NEO_LISTING"

NEO_METADATA=$(mktemp)
unzip -p "$NEOFORGE_JAR" META-INF/neoforge.mods.toml > "$NEO_METADATA"
rg -n 'modId = "magnetic"|modId = "minecraft"|modId = "yet_another_config_lib_v3"|version = "3\.12\.1-26\.2\+neoforge"' "$NEO_METADATA"
rm "$NEO_METADATA"
```

Expected: NeoForge metadata contains the Magnetic mod/version, Minecraft dependency, and YACL dependency; mixin and access-transformer files exist.

- [ ] **Step 5: Run the final scope and workspace audit**

Run:

```bash
git diff --check cbca955..HEAD
test -z "$(git diff --name-only cbca955..HEAD -- paper)"
test -z "$(git diff --name-only cbca955..HEAD -- CLAUDE.md)"
rg -n 'version\("26\.2-(fabric|neoforge)"' settings.gradle.kts
git status --short
```

Expected:

- no whitespace errors;
- no committed Paper or `CLAUDE.md` changes in the NeoForge activation range;
- both loader nodes print;
- the user's staged `CLAUDE.md` deletion and unstaged Nucleoid repository removal remain present.

- [ ] **Step 6: Report runtime follow-up without publishing**

Report that publishing tasks were not executed. Request NeoForge client/dedicated-server checks for:

```text
1. Magnetic initializes and creates config/magnetic.json.
2. /magnetic reload succeeds on a dedicated server.
3. Items and XP enter the player inventory.
4. The first and subsequent animated items move to the player and are picked up.
5. The NeoForge mod-list screen opens and saves the YACL configuration screen.
6. The Magnetic enchantment and its tags load from the packaged generated data.
```
