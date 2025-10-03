## Fabric

- fix bug when killing an animal, the exp was dropped every time, even with magnetic enabled
- remove side effects of break-chained blocks
    - previously, in rare circumstances, blocks that were broken by other entities were added to the player's inventory
      if they "tagged" the block with their profile before
    - this behaviour is now mitigated by only allowing a player to receive items from break-chains that were executed in
      less than 5 seconds

## Paper

- significantly improves break-chained block handling
    - now also includes drops that were triggered by another block that supported a structure, e.g. Cactus on top of
      Sand
    - replace broken kelp with water instead of air