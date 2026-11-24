# Automated testing

Magnetic's mod uses two automated test layers. JVM tests cover deterministic
logic; headless GameTests start Minecraft and verify behavior through the real
loaders and mixins. A successful compile is not a replacement for a GameTest
when mod code touches drops, entities, inventories, server ticks, or mixin
injection points. The Paper plugin has its own gameplay tests that run on
headless Paper and Folia servers; see [Paper gameplay tests](#paper-gameplay-tests).

## Commands

Run the smallest command that covers the change:

```bash
./gradlew testFast
./gradlew testGameLatest
./gradlew testGamePaper
./gradlew testAll
```

- `testFast` runs the JUnit suite for every Fabric and NeoForge target. It does
  not start a server. Use it during normal mod development.
- `testGameLatest` starts headless servers for the latest Fabric and NeoForge
  targets. Use it for mod gameplay, mixin, loader, inventory, XP, entity, or
  configuration-reload behavior.
- `testGamePaper` runs the Paper plugin's gameplay tests for every Paper
  target, each on the newest Paper release of its range and on Folia. Use it
  for any Paper plugin change; `:paper:<target>:runGameTest` runs one target.
- `testAll` builds every platform and runs the mod and Paper gameplay servers.
  Use it before release and for broad build-system changes.

Run a single target while diagnosing a failure:

```bash
./gradlew :mod:26.3-fabric:test
./gradlew :mod:26.3-fabric:runGameTest
./gradlew :mod:26.3-neoforge:runGameTest
./gradlew :paper:26.1:runGameTest
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
mod/src/gametestCommon/      Shared loader-independent scenarios and test data
mod/src/gametestFabric/      Fabric registration and fabric.mod.json
mod/src/gametestNeoForge/    NeoForge registration and neoforge.mods.toml
```

The common scenario currently verifies normal inactive drops, instant item
pickup, the item toggle, XP pickup, full-inventory drop safety, and deferred
pickup for auto-smelt datapacks. It breaks real blocks through `ServerPlayerGameMode`, so
the same production mixins used by a server are tested.

`mod/src/gametestCommon/resources` is packaged into both test mods. It holds
stand-in data for third-party datapacks, such as the Smelt Mining enchantment,
so compatibility can be tested without downloading the original pack.

Third-party pickup hooks, such as backpack pickup upgrades and pickup filters,
are emulated by `TestPickupHooks`. The Fabric test mod calls it from a mixin at
the head of `ItemEntity.playerTouch`, and the NeoForge test mod calls it from
`ItemEntityPickupEvent.Pre`; those are the hooks such mods use on each loader.
Install a hook only for the duration of one step with `TestPickupHooks.run`.

Test mods are separate source sets. Never move their classes or metadata into
`mod/src/main`; doing so would package test code in published artifacts.

Magnetic configuration is global to a server process, while GameTests may run
in parallel. Add config-mutating coverage as another step in
`MagneticGameTestScenario` unless the new test provides its own explicit
isolation. Always restore global state in a `finally` block. Use deterministic
blocks and loot; for example, coal ore is unsuitable for asserting positive XP
because zero XP is a valid result.

## Paper gameplay tests

The Paper plugin is tested by a separate test plugin in `paper/src/gametest`.
Like the plugin, it is built once per Paper target into
`paper/versions/<target>/build/gametest-libs`, never into the published jar.
`runGameTestPaper` and `runGameTestFolia` start a headless server with both
plugins on a fresh flat world, run every scenario and stop the server:

```bash
./gradlew :paper:26.1:runGameTestPaper   # Paper version from vers.gameTestPaperVersion
./gradlew :paper:26.1:runGameTestFolia   # Folia version from vers.foliaVersion
```

Both versions are set per target in `paper/versions/<target>/gradle.properties`.
Each target is compiled against the oldest Paper release of its range, so keep
`vers.gameTestPaperVersion` at the newest release of that range: the server
internals the plugin and the test plugin use must still work there. Code that
must differ inside a range cannot use a Stonecutter condition, because one jar
serves the whole range; use API or internals that exist across it instead.
Running these tasks accepts the Minecraft EULA for their throwaway servers
below `paper/versions/<target>/build/run/`.

Each server writes `magnetic-gametest-results.txt` next to its logs, and the
task fails unless every scenario passed.

`PaperGameTestScenarios` holds the scenarios. Each one joins a client-less
player through Paper's normal login path (`MockPlayers`), then acts through the
same server code a real player triggers: `Player#breakBlock`, entity damage,
item use, or entity interaction. Follow these rules when adding a scenario:

- Steps run at the end of a tick on the thread that owns the test site. That is
  where real players act, after the world ticked. Schedule follow-up steps with
  `TestContext.later` or `await` instead of the Bukkit schedulers.
- Every test gets a fresh player and a cleared site. Players stand three blocks
  from the drop so vanilla pickup cannot collect normal drops before they are
  counted.
- Configure Magnetic in memory with `ConfigKt.setConfig`; the runner restores
  the original config after each test. Change game rules or files only through
  `global` and `onCleanupGlobal`, and keep such tests at the end of the list.
- Use deterministic blocks and loot. When a drop is random, such as leaf decay,
  make the chance of no drop at all negligible.

The scenario "blocks that break with their support are collected" does not use
a fixed block list. It asks the server which blocks only stand on, or hang
from, their own kind, builds a column of each, breaks its base, and requires
every part that breaks with it, in the same tick or later, to be collected. New
blocks of a new Minecraft release are covered without editing the test.

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

Pushes and pull requests build every Stonecutter target in one matrix. The
latest Fabric and NeoForge targets and every Paper target also run their
`runGameTest`. Releases run
`testAll`. When an automated test fails,
fix the behavior or the deterministic assertion; do not make required tests
optional to get a green build.

Manual release checks remain appropriate for animation feel, configuration
screen usability, and third-party plugin or mod integrations that are not
present in the automated test runtime. Record new deterministic regressions in
JUnit, mod GameTests, or Paper gameplay tests so they do not return to the
manual checklist.
