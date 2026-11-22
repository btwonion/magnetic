## Fabric & Paper

- add support for Minecraft 26.3

## Paper

- remove automatic player drop deactivation when GraveX is installed
  - now users have to manually add "minecraft:player" as an ignored entity in the config
- fix Folia compatibility
  - bucket drops are now handed to the player's own region thread instead of being processed on the fluid's thread
  - the pull animation no longer speeds up with the number of active Folia regions
  - fix thread-safety issues in bucket tracking and full inventory alert cooldowns
- fix `buckets.abilityTimeout` being read as seconds instead of milliseconds
