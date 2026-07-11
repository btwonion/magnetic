# Single NeoForge Entrypoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the two NeoForge `@Mod` objects with the single distribution-guarded entrypoint pattern used by Better Boat Movement.

**Architecture:** `MagneticEntrypoint` keeps common config and command setup and reads `FMLLoader.getCurrent().dist`. Its `Dist.CLIENT` branch registers the YACL config-screen extension; the separate NeoForge client entrypoint file is deleted.

**Tech Stack:** Kotlin, Stonecutter, ModStitch, NeoForge FML, YACL, Gradle

## Global Constraints

- Use exactly one NeoForge `@Mod("magnetic")` entrypoint.
- Register `IConfigScreenFactory` only when `FMLLoader.getCurrent().dist == Dist.CLIENT`.
- Remove `MagneticClientEntrypoint` and `NeoForgeClient.kt`.
- Leave Fabric initialization, animation ticking, generated-data packaging, release orchestration, and Paper unchanged.
- Preserve the staged `CLAUDE.md` deletion and unstaged `mod/build.gradle.kts` repository edit exactly.
- Do not repair the local Java or Gradle environment if it is misconfigured.
- Do not add a permanent artifact-validation task.

---

### Task 1: Consolidate NeoForge Initialization

**Files:**
- Modify: `mod/src/main/kotlin/dev/nyon/magnetic/Main.kt:32-51`
- Delete: `mod/src/main/kotlin/dev/nyon/magnetic/NeoForgeClient.kt`

**Interfaces:**
- Consumes: `FMLLoader.getCurrent().dist`, `ModLoadingContext.get()`, `IConfigScreenFactory`, and `generateConfigScreen(parent)`.
- Produces: one `@Mod("magnetic") object MagneticEntrypoint` that performs common initialization on both distributions and config-screen registration on `Dist.CLIENT`.

- [ ] **Step 1: Run the structural test and verify the current implementation fails**

Run:

```bash
set -euo pipefail
count=$(rg -n '@Mod' \
  mod/src/main/kotlin/dev/nyon/magnetic/Main.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/NeoForgeClient.kt | wc -l)
test "$count" -eq 1
test ! -e mod/src/main/kotlin/dev/nyon/magnetic/NeoForgeClient.kt
```

Expected: FAIL because two `@Mod` declarations exist and `NeoForgeClient.kt` still exists.

- [ ] **Step 2: Move client initialization into the common entrypoint**

In the NeoForge Stonecutter branch of `Main.kt`, add the client imports and distribution guard:

```kotlin
import dev.nyon.magnetic.config.screen.generateConfigScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModLoadingContext
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
```

Keep the existing common setup, then add this after command registration inside `MagneticEntrypoint.init`:

```kotlin
when (loader.dist) {
    Dist.CLIENT -> {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _, parent -> generateConfigScreen(parent) }
        }
    }

    Dist.DEDICATED_SERVER -> Unit
}
```

Delete `mod/src/main/kotlin/dev/nyon/magnetic/NeoForgeClient.kt`. Do not change the animation-tick code.

- [ ] **Step 3: Re-run the structural test and verify it passes**

Run:

```bash
set -euo pipefail
test "$(rg -n '@Mod\("magnetic"\)' mod/src/main/kotlin/dev/nyon/magnetic/Main.kt | wc -l)" -eq 1
test ! -e mod/src/main/kotlin/dev/nyon/magnetic/NeoForgeClient.kt
! rg -n 'MagneticClientEntrypoint' mod/src/main
```

Expected: PASS with exactly one NeoForge entrypoint and no client-entrypoint file or symbol.

- [ ] **Step 4: Build every affected platform**

Run:

```bash
./gradlew :mod:26.2-neoforge:build :mod:26.2-fabric:build :paper:build
```

Expected: `BUILD SUCCESSFUL`. The known ModDevGradle capability-fallback warning for `26.2.0.10-beta` may remain.

- [ ] **Step 5: Inspect the loader jars once**

Run:

```bash
set -euo pipefail
fabric_jar=$(rg --files mod/versions/26.2-fabric/build/libs | awk '!/-sources\.jar$/ && /\.jar$/ {print; exit}')
neoforge_jar=$(rg --files mod/versions/26.2-neoforge/build/libs | awk '!/-sources\.jar$/ && /\.jar$/ {print; exit}')
test -s "$fabric_jar"
test -s "$neoforge_jar"

neo_entries=$(unzip -Z1 "$neoforge_jar")
printf '%s\n' "$neo_entries" | grep -Fxq 'dev/nyon/magnetic/MagneticEntrypoint.class'
! printf '%s\n' "$neo_entries" | grep -Fq 'MagneticClientEntrypoint'
! unzip -Z1 "$fabric_jar" | grep -Fq 'MagneticEntrypoint'

for jar in "$fabric_jar" "$neoforge_jar"; do
  entries=$(unzip -Z1 "$jar")
  for path in \
    data/magnetic/enchantment/magnetic.json \
    data/magnetic/tags/enchantment/auto_move.json \
    data/minecraft/tags/enchantment/in_enchanting_table.json \
    data/minecraft/tags/enchantment/tradeable.json \
    data/minecraft/tags/enchantment/treasure.json; do
    printf '%s\n' "$entries" | grep -Fxq "$path"
  done
  ! printf '%s\n' "$entries" | grep -q '\.cache'
done
```

Expected: PASS; NeoForge contains only `MagneticEntrypoint`, Fabric contains no NeoForge entrypoint, both jars contain all five generated-data resources, and neither jar contains `.cache`.

- [ ] **Step 6: Verify scope and commit only the implementation files**

Run:

```bash
test -z "$(git diff --name-only 386cb9c -- paper)"
git diff --check -- \
  mod/src/main/kotlin/dev/nyon/magnetic/Main.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/NeoForgeClient.kt
git status --short
git commit --only -m "refactor(neoforge): use single entrypoint" -- \
  mod/src/main/kotlin/dev/nyon/magnetic/Main.kt \
  mod/src/main/kotlin/dev/nyon/magnetic/NeoForgeClient.kt
git status --short
```

Expected: the commit contains only the entrypoint consolidation; status still shows `D  CLAUDE.md` and ` M mod/build.gradle.kts`.

