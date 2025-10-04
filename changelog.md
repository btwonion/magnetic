## Fabric

- fix a bug when killing an animal, the exp was dropped every time, even with magnetic enabled
- remove side effects of break-chained blocks
    - previously, in rare circumstances, blocks that were broken by other entities were added to the player's inventory
      if they "tagged" the block with their profile before
    - this behaviour is now mitigated by only allowing a player to receive items from break-chains that were executed in
      less than 5 seconds
- fix a bug with RightClickHarvest:
    - when right-clicking a sugar cane or cactus block before breaking it (left-click) before, the drops were dropped to
      the ground instead of using magnetic

## Paper

- significantly improves break-chained block handling
    - now also includes drops that were triggered by another block that supported a structure, e.g. Cactus on top of
      Sand
    - replace broken kelp with water instead of air

## Both

- add item-pulling animation
  - now every time an item is put into your inventory, there is an option to make it look like the item is pulled towards you (like you are the magnet :))
  - defaults: (enabled: true, blocksPerSecond: 1.0, canOtherPlayersPickup: false)