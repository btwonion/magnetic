# Multiloader Mod Migration Design

## Goal

Migrate the existing Fabric mod subproject to the Stonecutter and ModStitch multiloader structure used by [Better Boat Movement](https://github.com/btwonion/better-boat-movement), while leaving the Paper subproject's source code, build script, and properties unchanged.

The loader target is Fabric/Quilt plus NeoForge. For this migration, only the Fabric node is active and it continues to compile against Minecraft `26.2-pre-4`. NeoForge source branches and metadata are prepared, but the NeoForge version node is not registered until its version and dependency properties are supplied later.

## Project Layout

Rename the top-level `fabric/` directory to `mod/` because the subproject is no longer Fabric-specific. The resulting build has these boundaries:

```text
/
├── build.gradle.kts               # Root release orchestration
├── settings.gradle.kts            # Paper include and Stonecutter mod tree
├── paper/                          # Existing Paper subproject, unchanged
└── mod/
    ├── build.gradle.kts            # Shared ModStitch build
    ├── stonecutter.gradle.kts      # Stonecutter controller
    ├── src/main/                   # Shared and preprocessed mod sources
    └── versions/
        └── 26.2-fabric/
            └── gradle.properties   # Active Fabric version properties
```

Stonecutter is applied in root settings to the `:mod` project only. `:paper` remains a conventional Gradle subproject. The active version project is `:mod:26.2-fabric`, with Minecraft version metadata `26.2-pre-4`.

A NeoForge project is deliberately absent from the Stonecutter tree for now. It can be enabled later by adding `:mod:26.2-neoforge` to the tree and creating its `gradle.properties`. This prevents the current build from containing a project that is known to fail because its NeoForge coordinates are unspecified.

## Build and Release Configuration

The shared mod build follows the reference project:

- Stonecutter preprocesses loader-specific branches from `mod/src/main`.
- ModStitch selects Fabric Loom for the active Fabric node and will select ModDevGradle for a future NeoForge node.
- Loader-independent dependencies are declared once.
- Fabric API, Fabric Language Kotlin, and Mod Menu are selected only for Fabric.
- KotlinLangForge and NeoForge-specific YACL coordinates are selected only for NeoForge once the corresponding properties exist.
- Konfig is embedded for either loader.
- ModStitch's final jar task supplies publishing artifacts.
- The Fabric artifact retains Fabric and Quilt loader metadata on Modrinth and CurseForge.

The root `releaseAllPlatforms` task changes its mod dependency from `:fabric:releaseMod` to `:mod:26.2-fabric:releaseMod`. Release notification code reads supported Minecraft versions from the active mod version node. Paper's `:paper:releasePlugin` task remains unchanged.

Mod metadata moves from a concrete `fabric.mod.json` to ModStitch templates:

- `fabric.mod.json` contains the current Fabric entrypoints, dependencies, Mod Menu integration, and datagen entrypoint.
- `META-INF/neoforge.mods.toml` declares the future NeoForge mod and dependency ranges.

The existing `magnetic.classtweaker` remains the single access definition. ModStitch converts it to the Fabric access widener or NeoForge access transformer for the selected platform.

## Source Boundaries

Most existing mod code remains shared: mixins, configuration models and migration, drop handling, animation calculations, position tracking, generated data, resources, and config-screen construction.

Loader-specific code is confined to a small platform boundary selected with Stonecutter directives:

- mod initialization and config-directory lookup;
- command registration;
- level-tick registration;
- Fabric Mod Menu versus NeoForge config-screen registration;
- Fabric datagen classes and entrypoint;
- loader metadata templates.

Fabric initialization uses Fabric callbacks and `FabricLoader`. The dormant NeoForge branch follows the reference project's `@Mod` entrypoint structure and NeoForge event buses. It is retained as preprocessed source but is not compiled until a NeoForge version node exists.

`DropEvent` is an internal implementation detail: its Fabric `EventFactory` has no external subscribers. It is replaced with a loader-neutral handler invoked directly by the mixins and helper code. This removes an otherwise unnecessary Fabric API dependency from core drop behavior without changing its item, experience, animation, statistics, or inventory-alert semantics.

Fabric datagen providers remain Fabric-only source. Their already generated enchantment and tag JSON stays in shared runtime resources so both loader artifacts package identical game data.

## Failure Behavior

- Only registered Stonecutter nodes participate in Gradle configuration, so missing future NeoForge properties cannot break current work.
- Properties required by an active node fail configuration with the missing property name.
- Platform-specific dependencies and metadata are selected only for their loader.
- Stonecutter keeps inactive loader branches out of compiled source.
- Paper has no dependency on the Stonecutter controller or generated mod projects.

## Verification

Static verification will check:

- the Gradle project paths and active Stonecutter node;
- metadata substitutions and platform conditions;
- the class tweaker input and ModStitch conversion configuration;
- release task dependencies and supported-version lookup;
- the final Git diff to confirm no files under `paper/` changed.

Repository instructions prohibit the agent from running build or test commands. The handoff will therefore include commands for the user to run, including the active Fabric build and the Paper build.

Fabric in-game verification should cover:

- mod initialization and config creation;
- `/magnetic reload`;
- Mod Menu config-screen access;
- item and experience pickup;
- animated item movement and pickup;
- generated enchantment and tag data.

NeoForge build and runtime verification is deferred until the NeoForge node and its properties are supplied.

## Out of Scope

- Selecting or inventing NeoForge, KotlinLangForge, or NeoForge YACL version properties.
- Registering a currently unbuildable NeoForge version node.
- Changing Paper code, dependencies, metadata, or behavior.
- Changing Magnetic gameplay or configuration behavior beyond replacing the internal Fabric event wrapper with a loader-neutral handler.
