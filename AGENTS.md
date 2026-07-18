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

## Documentation

`docs/CONFIG.md` documents the end-user `magnetic.json` format.
