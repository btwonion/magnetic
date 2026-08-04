# Mod project guidance

## Structure

The mod is a Stonecutter multi-version project:

```text
mod/
├── src/main/                Shared, actively edited sources
├── generated/<version>/     Tracked datagen output
├── versions/<target>/       Target properties and generated build state
├── build.gradle.kts         Shared Fabric and NeoForge build configuration
└── stonecutter.gradle.kts   Active IDE target
```

Stonecutter evaluates `mod/build.gradle.kts` once for every target registered
in `settings.gradle.kts`. Target names combine a logical Minecraft version and
loader, such as `26.2-fabric` and `26.2-neoforge`.

`mod/src/main` is the shared source tree. Stonecutter conditional comments
select loader- or version-specific code while generating each target's build
sources. Files in `mod/versions/<target>/gradle.properties` provide Minecraft,
loader, and dependency versions.

Do not put tracked datagen output below `mod/src` or
`mod/versions/*/src`. Stonecutter manages source-set directories during
version switching and may relocate files found there.

## Selecting an active target

The active target controls IDE source state and is declared in
`mod/stonecutter.gradle.kts`. Switch it with Stonecutter's Gradle tasks or IDE
integration, and return to the VCS target before committing source-comment
changes:

```bash
./gradlew ':mod:Set active project to 26.2-fabric'
./gradlew ':mod:Reset active project'
```

Building does not require switching targets; Gradle generates sources for
every requested node.

## Building a target

Use the target project path:

```bash
./gradlew :mod:1.21.1-fabric:build
./gradlew :mod:1.21.1-neoforge:build
./gradlew :mod:26.2-fabric:build
./gradlew :mod:26.2-neoforge:build
```

Obfuscated Minecraft releases such as 1.21.1 use
`modstitch.platform=fabric-loom-remap` so the Fabric production jar is
remapped. Unobfuscated 26.x releases use `modstitch.platform=fabric-loom`.
Both publish the artifact produced by `modstitch.finalJarTask`.

Artifacts are written below each target's `build/libs` directory.

## Automated testing

Unit tests live in `mod/src/test` and are Stonecutter-generated for every
loader/version target. Keep parsing, timing, migration, and policy tests here;
use injected clocks rather than sleeps. Run all of them with:

```bash
./gradlew testFast
```

Headless gameplay tests are enabled for the latest Fabric and NeoForge targets.
Shared scenarios live in `mod/src/gametestCommon`; loader adapters and test-mod
metadata live in `mod/src/gametestFabric` and `mod/src/gametestNeoForge`.
They must exercise normal Minecraft actions so the production mixins are part
of the assertion path:

```bash
./gradlew testGameLatest
```

Magnetic's configuration is process-global and Minecraft may run GameTests in
parallel. Keep config-mutating assertions in the existing shared scenario, or
provide explicit isolation before introducing another scenario.

When the latest supported Minecraft version changes, move both GameTest source
sets in `mod/build.gradle.kts`, update the two target dependencies of the root
`testGameLatest` task, and adapt loader registration code if the GameTest API
changed. Test classes and metadata must remain outside `src/main` so they are
never packaged in release jars. Full details are in `docs/TESTING.md`.

## Data generation

Fabric is the canonical data generator for each Minecraft version. Matching
Fabric and NeoForge targets package the same tracked directory:

```text
mod/generated/<vers.versionName>/
```

Generate one version explicitly or every registered Fabric version:

```bash
./gradlew :mod:26.2-fabric:runDatagen
./gradlew runDatagen
```

Each Fabric task writes to a different logical-version directory, so
all-version generation does not mix outputs. Review and commit generated JSON;
`.cache` entries are ignored. NeoForge does not run a second generator—its
`processResources` task copies the matching directory.

## Adding a Minecraft version

1. Register Fabric and NeoForge target names and logical versions in
   `settings.gradle.kts`.
2. Add `mod/versions/<version>-fabric/gradle.properties` with Minecraft
   metadata, Fabric API, YACL, and Mod Menu. Use
   `modstitch.platform=fabric-loom-remap` for obfuscated Minecraft releases
   and `modstitch.platform=fabric-loom` for unobfuscated 26.x releases.
3. Add `mod/versions/<version>-neoforge/gradle.properties` with Minecraft
   metadata, NeoForge, YACL, KotlinLangForge, and
   `modstitch.platform=moddevgradle`.
4. Compile both targets, using the narrowest Stonecutter condition only when
   Minecraft APIs differ.
5. Run the Fabric target's `runDatagen` task.
6. Build both targets and confirm their jars contain identical generated
   resources.
7. Run the root `build` task before committing.

Use `vers.versionName` for the generated directory. This lets targets backed
by different exact game artifacts share data when they represent the same
logical release.
