# Paper project guidance

## Structure

The Paper plugin is a Stonecutter multi-version project:

```text
paper/
├── src/main/kotlin/dev/nyon/magnetic/
│   ├── compat/               Optional plugin integrations
│   ├── config/               Configuration and command handling
│   ├── extensions/           Paper-specific helpers
│   └── listeners/            Drop and block event handling
├── src/main/java/dev/nyon/magnetic/
│   ├── MagneticBootstrapper.java
│   └── MagneticLoader.java
├── src/main/resources/       Runtime resources and translations
├── src/gametest/             Test plugin with headless gameplay scenarios
├── versions/<target>/        Target properties and generated build state
├── build.gradle.kts          Paper build, run, test, and publishing configuration
└── stonecutter.gradle.kts    Active IDE target
```

The Paper implementation is independent of the mod source tree. Keep
Paper-specific behavior here, and place optional plugin integrations in
`compat/`. Veinminer, mcMMO, AuraSkills, Tree Feller, and TimberZ are compile-only
dependencies and must remain optional at runtime.

Register every integration in `compat/PluginCompats.kt`, which checks the
required plugin or class before the integration's code is loaded. Use
`collectDrop` from `compat/CompatDrops.kt` to collect single stacks or item
entities that another plugin drops.

`listeners/SupportBreakListeners.kt` collects blocks that break because the
player broke what they stood on, hung from, or were attached to: torches and
carpets in the same tick, sugar cane, vines, or big dripleaf a few ticks later.
Vanilla destroys them through `Level#destroyBlock`, which fires Paper's
`BlockDestroyEvent`; the player's own `BlockDropItemEvent` only holds the
broken block's drops. The listener needs no block list: vanilla's placement
rules decide which neighbors break.

## Targets

Stonecutter evaluates `paper/build.gradle.kts` once for every target registered
in `settings.gradle.kts`. Each target publishes one jar for a range of Paper
releases. It is compiled against the oldest release of its range and its
gameplay tests run on the newest:

| Target   | Dev bundle (oldest) | Supports             | Java | Gameplay tests |
|----------|---------------------|----------------------|------|----------------|
| `1.21.1` | 1.21.1              | 1.21 – 1.21.5        | 21   | 1.21.5         |
| `1.21.6` | 1.21.6              | 1.21.6 – 1.21.11     | 21   | 1.21.11        |
| `26.1`   | 26.1.1              | 26.1 – 26.3          | 25   | 26.3           |

The ranges follow Paper API breaks. 1.21 is the first release with a
data-driven enchantment registry. 1.21.6 renamed the registry event that
registers the Magnetic enchantment from `freeze()` to `compose()`. 26.1 requires
Java 25. `paper/versions/<target>/gradle.properties` holds each target's
versions as `vers.*` properties.

`paper/src` is the shared source tree. Stonecutter conditional comments such
as `//? if >=1.21.6` select version-specific code when generating each target's
sources. A condition can only distinguish targets, not releases inside one
target's range, because one jar serves the whole range. Inside a range, use
Paper API or server internals that exist on every release of it; the gameplay
tests on the newest release catch internals that were renamed or removed.

The active target controls IDE source state and is declared in
`paper/stonecutter.gradle.kts`. Switch it with Stonecutter's Gradle tasks and
return to the VCS target before committing:

```bash
./gradlew ':paper:Set active project to 1.21.1'
./gradlew ':paper:Reset active project'
```

## Building and running

Build one target or every target:

```bash
./gradlew :paper:26.1:build
./gradlew build
```

Run a local Paper or Folia server for a target:

```bash
./gradlew :paper:26.1:runServer
./gradlew :paper:26.1:runFolia
```

Artifacts are written below `paper/versions/<target>/build/libs`.

## Verification

Build the plugin and run its gameplay tests on headless Paper and Folia
servers, for one target or every target:

```bash
./gradlew :paper:26.1:build
./gradlew :paper:26.1:runGameTest
./gradlew testGamePaper
```

The tests live in `src/gametest`, a separate test plugin that is never
packaged into the published jar. Add a scenario to `PaperGameTestScenarios`
for every behavior change or regression that a server-side player can trigger,
and follow the rules in `docs/TESTING.md`. Optional plugin integrations are
not present in the test runtime; check them, and anything visual, manually
with `runServer` or `runFolia`.

## Adding a Minecraft version

- **Inside an existing range** (the API is unchanged): add the release to
  that target's `vers.supportedMcVersions`, move `vers.gameTestPaperVersion`
  (and `vers.foliaVersion`, if Folia published it) to it, and run the target's
  `runGameTest`.
- **New target** (the API broke):
  1. Register the target in the `paper` tree in `settings.gradle.kts`.
  2. Add `paper/versions/<target>/gradle.properties` with the dev bundle,
     supported releases, Java version, Folia and gameplay-test versions, and an
     unused `vers.gameTestPort` (Folia uses the next port).
  3. Compile the target, using the narrowest Stonecutter condition only where
     the Paper API or server internals differ.
  4. Add the target to the `build` matrix in
     `.github/workflows/build-commit.yml` and to the table above.
  5. Run `./gradlew build testGamePaper` before committing.

In both cases, the gameplay test "blocks that break with their support are
collected" checks the release's new blocks: it builds a column of every block
that the server's placement rules only allow on its own kind, breaks its base,
and fails when a part that broke with it stays on the ground. A new block that
breaks without going through `Level#destroyBlock`, for example by falling like
a stalactite, needs its own handling.
