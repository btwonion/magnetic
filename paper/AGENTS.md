# Paper project guidance

## Structure

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
├── gradle.properties         Paper and Minecraft versions
└── build.gradle.kts          Paper build, run, and publishing configuration
```

The Paper implementation is independent of the mod source tree. Keep
Paper-specific behavior here, and place optional plugin integrations in
`compat/`. Veinminer, mcMMO, AuraSkills, Tree Feller, and TimberZ are compile-only
dependencies and must remain optional at runtime.

Register every integration in `compat/PluginCompats.kt`, which checks the
required plugin or class before the integration's code is loaded. Use
`collectDrop` from `compat/CompatDrops.kt` to collect single stacks or item
entities that another plugin drops.

## Building and running

Build the plugin:

```bash
./gradlew :paper:build
```

Run a local Paper or Folia server:

```bash
./gradlew :paper:runServer
./gradlew :paper:runFolia
```

The plugin artifact is written below `paper/build/libs`.

## Verification

Build the plugin and run its gameplay tests on headless Paper and Folia
servers:

```bash
./gradlew :paper:build
./gradlew :paper:testGame
```

The tests live in `src/gametest`, a separate test plugin that is never
packaged into the published jar. Add a scenario to `PaperGameTestScenarios`
for every behavior change or regression that a server-side player can trigger,
and follow the rules in `docs/TESTING.md`. Optional plugin integrations are
not present in the test runtime; check them, and anything visual, manually
with `runServer` or `runFolia`.
