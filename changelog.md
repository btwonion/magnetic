## Fabric, NeoForge & Paper

- ignore the ender dragon by default so its experience drops normally
  - only applies to new configs; existing configs keep their current `ignoreEntities` list

## Fabric & NeoForge

- add compatibility with mods that hook item pickup, such as Sophisticated Backpacks' pickup upgrades and pickup filters like FiltPick
  - collected drops now go through vanilla item pickup, so a backpack can take them even when the inventory is full
  - drops a pickup filter refuses stay on the ground, without a full inventory alert
  - pulled items that a filter keeps refusing stop following the player after one second
- add [Server Tree Feller](https://modrinth.com/mod/server-tree-feller) compatibility
  - logs it breaks over the following ticks are now collected by the player who felled the tree
- improve [HT's TreeChop](https://modrinth.com/mod/treechop) compatibility
  - every felled block is checked against the ignored blocks list individually instead of only the chopped log
- fix the pull animation running faster on Fabric for every loaded dimension
- fix items and experience from ignored entities being collected when the kill happens inside another player action, such as a melee hit on 1.21.x
- fix full inventory alerts showing raw translation keys to players without the mod on server-only installs ([**#114**](https://github.com/btwonion/magnetic/pull/114))
- fix the Fabric version crashing on startup when loaded through Sinytra Connector ([**#77**](https://github.com/btwonion/magnetic/issues/77))

## Paper

- add [ThizzyZ Tree Feller](https://modrinth.com/plugin/thizzyz-tree-feller) compatibility
- fix plugin failing to enable when a different plugin named "Veinminer" is installed
- fix Folia compatibility
  - bucket drops are now handed to the player's own region thread instead of being processed on the fluid's thread
  - the pull animation no longer speeds up with the number of active Folia regions
  - fix thread-safety issues in bucket tracking and full inventory alert cooldowns
- fix `buckets.abilityTimeout` being read as seconds instead of milliseconds
