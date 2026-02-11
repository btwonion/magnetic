## Both loaders
- update default config values [**#66**](https://github.com/btwonion/magnetic/issues/66)
  - ignoredEntitiesRangeMin: 15 -> 50
  - titleAlert - enabled: true -> false
- capitalize enchantment names everywhere [**#64**](https://github.com/btwonion/magnetic/issues/64)
- do not animate items when the player's inventory is full [**#65**](https://github.com/btwonion/magnetic/issues/65)

## Fabric
- handle container blocks again
  - also adds drops of decorated pots [**#68**](https://github.com/btwonion/magnetic/issues/68)

## Paper

- fix console log spam that occurs when the animation is enabled and an item and a player are in different
  worlds [**#71**](https://github.com/btwonion/magnetic/issues/71)
- ignore player drops on death if GravesX is present [**#73**](https://github.com/btwonion/magnetic/issues/73)
- init tags before flattening the registry
- fix enchantment registration on first launch after updating to v3.9.0 [**#69**](https://github.com/btwonion/magnetic/issues/69), [**#70**](https://github.com/btwonion/magnetic/issues/70)