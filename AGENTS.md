# Repository guidance

Magnetic is a Gradle multi-project build for a Minecraft mod and Paper plugin.

## Project structure

```text
.
├── common/                 Platform-independent config, conditions and cooldowns
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
./gradlew testFast         # JVM tests for common and every mod target
./gradlew testGameLatest   # headless Fabric and NeoForge gameplay tests
./gradlew testGamePaper    # headless Paper and Folia gameplay tests
./gradlew testAll          # full build plus every test layer
```

Use `testFast` while editing pure logic, add `testGameLatest` for mod gameplay,
mixins, inventory, entity, or loader changes, run `testGamePaper` for Paper
plugin changes, and run `testAll` before release. See `docs/TESTING.md` for
test ownership, source layout, and extension rules.

## Comments

Do not add comments, KDoc or Javadoc that restate what the code, names,
version numbers or a quick look at an error already tell the reader. This
applies to every source set, the `common` module, build scripts and
`gradle.properties`. Examples of comments not to write:

- `// Shared classes ship inside the mod jar.`
- `# KotlinLangForge's 3.1 flavor targets newer FML; NeoForge 21.1 needs 3.0.`
- `/** Parses namespace:path, path or either with a leading #. */`

Comment only a non-obvious workaround that a reader could not work out from
the surrounding code. Stonecutter conditions (`//?`, `/*?`) and the code they
disable are not comments and stay. Check the diff for added comments before
finishing a change.

## Shared code

`common` holds code that needs no Minecraft or server API: the `magnetic.json`
model and its migrations, condition statement parsing, identifiers, and
cooldowns. The mod and the Paper plugin copy its classes into their own jars,
so it may only use the Kotlin standard library and kotlinx-serialization, which
every platform provides. Put behavior both platforms share here instead of
duplicating it, and keep its JUnit tests in `common/src/test`.

## Documentation

`docs/CONFIG.md` documents the end-user `magnetic.json` format.
