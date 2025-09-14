# Fabric & Paper

- add an option to require a player to have a permission ('magnetic.ability.use') to use the ability
  - `permissionRequired`, by default: false
  - on Fabric the fabric-permissions-api has to be installed for this to work
- add an option to ignore drops from entities when killed
  - `ignoreEntities`, by default: none
  - the option takes identifiers and tags as an input
- add an option to ignore drops produced by ranged weapons
  - `ignoreRangedWeapons`, by default: true
- bump config version to 2:
  - `needEnchantment` -> `enchantmentRequired`
  - `needSneak` -> `sneakRequired`
  - reorder values
  - -> the migration is done automatically
  - also improve config documentation in README and config screen

# Paper
- fixed an issue, where the enchantment was created, even though the corresponding option was set to `false`
- now also handle break-chained blocks like Sugar Cane, Bamboo, etc.