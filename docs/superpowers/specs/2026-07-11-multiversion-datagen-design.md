# Multi-Version Datagen and Minecraft 26.1.2 Design

## Goal

Add Fabric and NeoForge targets for Minecraft 26.1.2 and make generated data safe to produce and package across multiple Minecraft versions. Fabric remains the canonical data generator for each Minecraft version, while both loaders package the same version-specific output.

## Project Targets

The `mod` Stonecutter branch will contain four nodes:

- `26.1.2-fabric`, logical Minecraft version `26.1.2`
- `26.1.2-neoforge`, logical Minecraft version `26.1.2`
- `26.2-fabric`, logical Minecraft version `26.2-pre-4`
- `26.2-neoforge`, logical Minecraft version `26.2`

The 26.1.2 nodes use these pinned properties:

| Property | Fabric value | NeoForge value |
| --- | --- | --- |
| `vers.mcVersion` | `26.1.2` | `26.1.2` |
| `vers.versionName` | `26.1.2` | `26.1.2` |
| `vers.mcVersionRange` | `26.1.2` | `26.1.2` |
| `vers.supportedMcVersions` | `26.1.2` | `26.1.2` |
| `vers.deps.fapi` | `0.154.2+26.1.2` | not applicable |
| `vers.deps.modMenu` | `18.0.0` | not applicable |
| `vers.deps.fml` | not applicable | `26.1.2.78` |
| `vers.deps.yacl` | `3.9.5+26.1-fabric` | `3.9.5+26.1-neoforge` |
| `vers.deps.klf` | not applicable | `3.1` |
| `modstitch.platform` | `fabric-loom` | `moddevgradle` |

The project-wide Fabric Loader and Fabric Language Kotlin versions remain sourced from `gradle/libs.versions.toml`.

## Generated Resource Layout

Generated files live outside every Stonecutter-managed `src` tree:

```text
mod/generated/
├── 26.1.2/
│   └── data/...
└── 26.2/
    └── data/...
```

The path is derived from `vers.versionName`, not `vers.mcVersion`. This gives preview-backed builds such as `26.2-fabric` and released builds such as `26.2-neoforge` one shared logical output directory.

The existing tracked files in `mod/src/main/generated` move without content changes to `mod/generated/26.2`. Datagen cache files remain untracked through the existing `**/.cache` ignore rule.

Keeping generated output outside `src` is required. Stonecutter registers source-set directories for version switching and may relocate a generated directory nested under `src` into an active version node.

## Gradle Data Flow

The central `mod/build.gradle.kts` derives one directory for each evaluated node:

```kotlin
val generatedResources =
    rootProject.layout.projectDirectory.dir("mod/generated/$mcVersionName")
```

For Fabric nodes, `FabricApiExtension.configureDataGeneration` sets `outputDirectory` to `generatedResources.asFile` and keeps `client = true`. Fabric Loom registers that output as a Fabric resource input and provides `runDatagen`.

For NeoForge nodes, `processResources` copies from `generatedResources` and excludes `.cache/**`. NeoForge does not run its own data providers; its artifact consumes the output produced by the matching Fabric node.

Because every logical Minecraft version has a distinct output directory, these workflows are both safe:

```bash
./gradlew :mod:26.1.2-fabric:runDatagen
./gradlew runDatagen
```

The targeted form regenerates one version. The unqualified form fans out to all Fabric nodes, whose outputs do not collide.

## Source Compatibility

Adding 26.1.2 must not fork the shared implementation. Compilation or mapping differences between 26.1.2 and 26.2 are handled with the smallest possible Stonecutter conditional around the affected code. Loader-only differences continue to use the existing `fabric` and `neoforge` constants.

No unrelated source refactoring is part of this work. The existing uncommitted change in `BucketItemMixin.java` must be preserved and excluded from design, plan, and implementation commits unless it is independently required for 26.1.2 compatibility and the user explicitly includes it.

## Developer Documentation

Create `docs/DEVELOPMENT.md` as the developer-facing project guide. It documents:

- the root project, `mod`, `paper`, and Stonecutter version-node structure;
- shared source directories and version-specific Gradle properties;
- the difference between Stonecutter-generated build sources and tracked datagen output;
- Fabric as the canonical data generator for both loaders;
- the `mod/generated/<vers.versionName>` convention;
- targeted and all-version build and datagen commands;
- the procedure for adding another Minecraft version and loader pair;
- the rule that tracked datagen output must remain outside Stonecutter-managed `src` directories.

`docs/CONFIG.md` remains unchanged because it documents end-user mod configuration rather than repository development.

## Validation

Implementation is complete when all of the following hold:

1. `./gradlew :mod:26.1.2-fabric:runDatagen` succeeds and writes tracked data only under `mod/generated/26.1.2`.
2. No `mod/versions/*/src/main/generated` directory is created.
3. `./gradlew runDatagen` succeeds for all Fabric versions without output collisions.
4. `./gradlew build` succeeds for all four mod variants and the Paper module.
5. The Fabric and NeoForge 26.1.2 jars contain the same generated enchantment and enchantment-tag resources.
6. The 26.2 generated resources remain packaged by both existing 26.2 variants after their directory migration.
7. A final Git diff contains only intended Gradle configuration, version properties, compatibility edits, generated resources, and developer documentation.

Dependency coordinates are pinned and must fail normally during Gradle resolution if unavailable; the build must not silently fall back to another Minecraft or loader version.
