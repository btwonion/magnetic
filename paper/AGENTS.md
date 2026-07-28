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
├── gradle.properties         Paper and Minecraft versions
└── build.gradle.kts          Paper build, run, and publishing configuration
```

The Paper implementation is independent of the mod source tree. Keep
Paper-specific behavior here, and place optional plugin integrations in
`compat/`. Veinminer, mcMMO, and AuraSkills are compile-only dependencies and
must remain optional at runtime.

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
