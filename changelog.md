# Fabric & Paper

- add an option to require a player to have a permission ('magnetic.ability.use') to use the ability ([**#47**](https://github.com/btwonion/magnetic/pull/47))
    - `permissionRequired`, by default: false
    - on Fabric the fabric-permissions-api has to be installed for this to work
- add an option to ignore drops from entities when killed ([**#48**](https://github.com/btwonion/magnetic/pull/48))
    - `ignoreEntities`, by default: none
    - the option takes identifiers and tags as an input
- add an option to ignore drops produced by ranged weapons ([**#49**](https://github.com/btwonion/magnetic/pull/49))
    - `ignoreRangedWeapons`, by default: true
- add an option to play/show sound and text message alerts when the inventory is full ([**#51**](https://github.com/btwonion/magnetic/pull/51))
    - `fullInventoryAlert`, by default: both enabled, cooldowns: sound - 5s; messages - 60s
    - the alerts will show when magnetic tries to add an item to the player's inventory, but the inventory is already
      full
    - add a new `plugins/magnetic/translations` folder on Paper, where the localization messages can be configured
- bump config version to 2:
    - `needEnchantment` -> `enchantmentRequired`
    - `needSneak` -> `sneakRequired`
    - reorder values
    - -> the migration is done automatically
    - also improve config documentation in README and config screen

# Paper

- fixed an issue, where the enchantment was created, even though the corresponding option was set to `false`
- now also handle break-chained blocks like Sugar Cane, Bamboo, etc. ([**#50**](https://github.com/btwonion/magnetic/pull/50))