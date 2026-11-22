## Fabric, NeoForge & Paper

- ignore the ender dragon by default so its experience drops normally
  - only applies to new configs; existing configs keep their current `ignoreEntities` list

## Fabric & NeoForge

- fix items and experience from ignored entities being collected when the kill happens inside another player action, such as a melee hit on 1.21.x

## Fabric & Paper

- add support for Minecraft 26.3

## Paper

- remove automatic player drop deactivation when GraveX is installed
  - now users have to manually add "minecraft:player" as an ignored entity in the config
- fix plugin failing to enable when a different plugin named "Veinminer" is installed
  - Veinminer compatibility is now only enabled for Miraculixx's Veinminer, otherwise a warning is logged
