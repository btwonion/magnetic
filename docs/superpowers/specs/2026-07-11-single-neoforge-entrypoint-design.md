# Single NeoForge Entrypoint Design

## Goal

Match Better Boat Movement's NeoForge initialization pattern by using one
`@Mod("magnetic")` entrypoint for both common and client initialization.

## Architecture

`MagneticEntrypoint` remains in `Main.kt`. It initializes the shared config and
registers dedicated-server commands for every physical distribution. It also
reads `FMLLoader.getCurrent().dist` and, only for `Dist.CLIENT`, registers the
YACL config-screen extension through `ModLoadingContext` and
`IConfigScreenFactory`.

The separate `MagneticClientEntrypoint` and `NeoForgeClient.kt` are removed.
The result must contain exactly one NeoForge `@Mod` entrypoint for the
`magnetic` mod ID.

Fabric initialization, lazy animation-tick registration, generated-data
packaging, release orchestration, and the Paper module remain unchanged.

## Verification

- A structural check must first demonstrate that the current source has two
  NeoForge `@Mod` entrypoints, then pass after consolidation with exactly one.
- NeoForge, Fabric, and Paper builds must succeed.
- The NeoForge jar must contain `MagneticEntrypoint.class` and must not contain
  `MagneticClientEntrypoint.class`.
- The Fabric jar must not contain NeoForge entrypoint classes.
- Both loader jars must retain the five committed generated-data resources and
  exclude `.cache` entries.
- The user's staged `CLAUDE.md` deletion and unstaged repository edit must be
  preserved exactly.

## Known Constraint

No NeoForge run task is currently available, so real client and dedicated-server
startup remain manual pre-release smoke tests. This matches the accepted BBM
entrypoint approach but does not replace runtime testing.
