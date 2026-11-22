## Fabric, NeoForge & Paper

- ignore the ender dragon by default so its experience drops normally
  - only applies to new configs; existing configs keep their current `ignoreEntities` list

## Fabric & NeoForge

- fix items and experience from ignored entities being collected when the kill happens inside another player action, such as a melee hit on 1.21.x
- fix full inventory alerts showing raw translation keys to players without the mod on server-only installs ([**#114**](https://github.com/btwonion/magnetic/pull/114))
- fix the Fabric version crashing on startup when loaded through Sinytra Connector ([**#77**](https://github.com/btwonion/magnetic/issues/77))

## Paper

- fix plugin failing to enable when a different plugin named "Veinminer" is installed
- fix Folia compatibility
  - bucket drops are now handed to the player's own region thread instead of being processed on the fluid's thread
  - the pull animation no longer speeds up with the number of active Folia regions
  - fix thread-safety issues in bucket tracking and full inventory alert cooldowns
- fix `buckets.abilityTimeout` being read as seconds instead of milliseconds
