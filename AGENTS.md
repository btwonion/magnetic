# Repository guidance

Magnetic is a Gradle multi-project build for a Minecraft mod and Paper plugin.

## Project structure

```text
.
├── mod/                    Fabric and NeoForge mod; see `mod/AGENTS.md`
├── paper/                  Paper plugin; see `paper/AGENTS.md`
├── docs/                   End-user documentation
├── settings.gradle.kts     Subproject and Stonecutter target registration
└── build.gradle.kts        Root aggregation and release tasks
```

Read the nearest nested `AGENTS.md` before changing a subproject.

## General commands

Build every mod target and the Paper plugin:

```bash
./gradlew build
```

Build artifacts are written below each subproject's `build/libs` directory.

Run automated verification at the narrowest useful layer:

```bash
./gradlew testFast        # JVM tests for every mod target
./gradlew testGameLatest  # headless Fabric and NeoForge gameplay tests
./gradlew testAll         # full build plus every mod test layer
```

Use `testFast` while editing pure logic, add `testGameLatest` for gameplay,
mixins, inventory, entity, or loader changes, and run `testAll` before release.
See `docs/TESTING.md` for test ownership, source layout, and extension rules.
Automated tests belong to the mod project; verify Paper changes with
`./gradlew :paper:build` and, when needed, a local server run.

## Documentation

`docs/CONFIG.md` documents the end-user `magnetic.json` format.
