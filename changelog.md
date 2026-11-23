## Fabric, NeoForge & Paper

- ignore the ender dragon by default so its experience drops normally
  - only applies to new configs; existing configs keep their current `ignoreEntities` list
- add compatibility with auto-smelt datapacks that smelt dropped items, also when mining with Veinminer
  - supports Smelting Enchantment, Veinmine & Autosmelt Enchantments, Smelt Mining, and Smelt Enchant
  - collected drops are smelted with the same rules and experience as the datapack; drops that do not fit stay unsmelted so the datapack still handles them

## Fabric & NeoForge

- fix items and experience from ignored entities being collected when the kill happens inside another player action, such as a melee hit on 1.21.x
- fix full inventory alerts showing raw translation keys to players without the mod on server-only installs ([**#114**](https://github.com/btwonion/magnetic/pull/114))
- fix the Fabric version crashing on startup when loaded through Sinytra Connector ([**#77**](https://github.com/btwonion/magnetic/issues/77))

## Paper

- add compatibility with auto-smelt plugins, including on blocks broken by Veinminer
  - items and experience that plugins such as AutoSmelt spawn themselves while a block is broken are now collected
  - block and Veinminer drops are now collected after other plugins changed them, so ExcellentEnchants' Smelter and SuperEnchants' autosmelt deliver smelted items
- fix partially collected Veinminer drops being dropped in full
- fix plugin failing to enable when a different plugin named "Veinminer" is installed
- fix Folia compatibility
  - bucket drops are now handed to the player's own region thread instead of being processed on the fluid's thread
  - the pull animation no longer speeds up with the number of active Folia regions
  - fix thread-safety issues in bucket tracking and full inventory alert cooldowns
- fix `buckets.abilityTimeout` being read as seconds instead of milliseconds
