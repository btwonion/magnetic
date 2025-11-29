## Fabric & Paper
- change `ignoreRangedWeapons` to `ignoredEntitiesRangeMin`
  - now the ignored drops are calculated by the distance the mob has died in comparison to the player
  - if the value is -1, no check will be performed
  - type: double, default: 15.0
