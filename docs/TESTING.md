# Automated testing

Magnetic's mod uses two automated test layers. JVM tests cover deterministic
logic; headless GameTests start Minecraft and verify behavior through the real
loaders and mixins. A successful compile is not a replacement for a GameTest
when mod code touches drops, entities, inventories, server ticks, or mixin
injection points. The Paper project has no automated test suite; verify it with
`./gradlew :paper:build` and manual server runs when appropriate.

## Commands

Run the smallest command that covers the change:

```bash
./gradlew testFast
./gradlew testGameLatest
./gradlew testAll
```

- `testFast` runs the JUnit suite for every Fabric and NeoForge target. It does
  not start a server. Use it during normal mod development.
- `testGameLatest` starts headless servers for the latest Fabric and NeoForge
  targets. Use it for mod gameplay, mixin, loader, inventory, XP, entity, or
  configuration-reload behavior.
- `testAll` builds every platform and runs both mod gameplay servers. Use it
  before release and for broad build-system changes.

Run a single target while diagnosing a failure:

```bash
./gradlew :mod:26.2-fabric:test
./gradlew :mod:26.2-fabric:runGameTest
./gradlew :mod:26.2-neoforge:runGameTest
```

JUnit HTML reports are below each target's `build/reports/tests`. GameTest logs
are below that target's `build/run/gameTest/logs`. CI uploads reports and logs
when verification fails.

## JVM tests

JVM tests are stored in `mod/src/test/kotlin`. Stonecutter preprocesses this
source set for every target. Fabric uses Modstitch's loader-aware JUnit
environment; NeoForge deliberately uses plain JUnit so its suite remains
compatible with every supported loader version.

Prefer JVM tests for:

- condition parsing and left-to-right truth tables;
- config serialization and migrations;
- cooldown and timeout behavior;
- identifier parsing and deterministic pickup policy;
- regressions that can be represented without a world or real player.

Do not sleep in a test. Inject a clock, advance it directly, and assert the
boundary instants. NeoForge JVM tests must not depend on loader initialization.
Avoid large mocks of `ServerPlayer`, `ServerLevel`, or registries; those belong
in a GameTest.

## Gameplay tests

Gameplay tests use three source roots:

```text
mod/src/gametestCommon/      Shared loader-independent scenarios
mod/src/gametestFabric/      Fabric registration and fabric.mod.json
mod/src/gametestNeoForge/    NeoForge registration and neoforge.mods.toml
```

The common scenario currently verifies normal inactive drops, instant item
pickup, the item toggle, XP pickup, and full-inventory drop safety. It breaks
real blocks through `ServerPlayerGameMode`, so the same production mixins used
by a server are tested.

Test mods are separate source sets. Never move their classes or metadata into
`mod/src/main`; doing so would package test code in published artifacts.

Magnetic configuration is global to a server process, while GameTests may run
in parallel. Add config-mutating coverage as another step in
`MagneticGameTestScenario` unless the new test provides its own explicit
isolation. Always restore global state in a `finally` block. Use deterministic
blocks and loot; for example, coal ore is unsuitable for asserting positive XP
because zero XP is a valid result.

## Moving tests to a new latest target

Headless gameplay coverage intentionally follows the latest supported target;
JUnit and compilation coverage still run for every registered target. When a
new latest Minecraft version is added:

1. Update the `mcVersionName` guards around GameTest source-set configuration
   in `mod/build.gradle.kts`.
2. Update the Fabric and NeoForge project paths used by `testGameLatest` in the
   root `build.gradle.kts`.
3. Compile both GameTest source sets.
4. Adapt the thin loader registration classes if either loader changed its
   GameTest API. Keep the assertions in `gametestCommon` when vanilla APIs
   remain compatible.
5. Run `./gradlew testAll` and confirm release jars do not contain
   `dev/nyon/magnetic/gametest` or the `magnetic_test` metadata.

## CI and manual checks

Pushes and pull requests use a target matrix: every Fabric and NeoForge target
and Paper build independently, while the latest Fabric and NeoForge entries
also run their GameTests. Releases run `testAll`. When an automated test fails,
fix the behavior or the deterministic assertion; do not make required tests
optional to get a green build.

Manual release checks remain appropriate for Paper behavior, animation feel,
configuration screen usability, and third-party compatibility profiles that
are not present in the automated test runtime. For the mod, record new
deterministic regressions in JUnit or GameTests so they do not return to the
manual checklist.
