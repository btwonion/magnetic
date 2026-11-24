# Repository guidance

Magnetic is a Gradle multi-project build for a Minecraft mod and Paper plugin.

## Project structure

```text
.
├── mod/                    Fabric and NeoForge mod; see `mod/AGENTS.md`
├── paper/                  Paper plugin (Stonecutter); see `paper/AGENTS.md`
├── buildSrc/               Versioning and publishing shared by mod and Paper
├── docs/                   End-user documentation
├── settings.gradle.kts     Subproject and Stonecutter target registration
└── build.gradle.kts        Root aggregation and release tasks
```

Read the nearest nested `AGENTS.md` before changing a subproject.

## General commands

Build every mod and Paper target:

```bash
./gradlew build
```

Build artifacts are written below each subproject's `build/libs` directory.

Run automated verification at the narrowest useful layer:

```bash
./gradlew testFast         # JVM tests for every mod target
./gradlew testGameLatest   # headless Fabric and NeoForge gameplay tests
./gradlew testGamePaper    # headless Paper and Folia gameplay tests
./gradlew testAll          # full build plus every test layer
```

Use `testFast` while editing pure logic, add `testGameLatest` for mod gameplay,
mixins, inventory, entity, or loader changes, run `testGamePaper` for Paper
plugin changes, and run `testAll` before release. See `docs/TESTING.md` for
test ownership, source layout, and extension rules.

## Documentation

`docs/CONFIG.md` documents the end-user `magnetic.json` format.
