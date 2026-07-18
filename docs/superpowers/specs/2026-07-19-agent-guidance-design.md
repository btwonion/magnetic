# Agent guidance consolidation

## Goal

Replace `docs/DEVELOPMENT.md` with hierarchical agent guidance that keeps
repository-wide information at the root and places subproject details beside
the code they describe.

## File layout

- `AGENTS.md` describes the broad repository structure, documentation scope,
  and repository-wide commands such as `./gradlew build`.
- `mod/AGENTS.md` describes the shared mod source tree, Stonecutter targets,
  target-specific builds, data generation, and adding Minecraft versions.
- `paper/AGENTS.md` describes the Paper plugin source layout and Paper-specific
  build and run commands.
- `docs/DEVELOPMENT.md` is removed.

## Content rules

- Do not duplicate subproject details in the root file.
- Preserve the useful development guidance from `docs/DEVELOPMENT.md` in the
  appropriate scoped file.
- Keep commands at the narrowest applicable scope.
- Keep `docs/CONFIG.md` as the end-user configuration reference.

## Verification

- Confirm `docs/DEVELOPMENT.md` no longer exists.
- Confirm all three `AGENTS.md` files exist and contain only guidance for their
  scopes.
- Search the repository for stale `docs/DEVELOPMENT.md` references.
- Review the final diff for accidental unrelated changes.
