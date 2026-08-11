## Fabric, NeoForge & Paper

- make Magnetic enchanted books available from desert librarians when the Villager Trade Rebalance experiment is enabled
- add an optional collection of natural leaf-decay drops ([**#99**](https://github.com/btwonion/magnetic/pull/99))
    - disabled by default through the new `leafDecay` config section
    - preserves eligibility from the original log break and supports accelerated decay mods
    - expires player attribution after a configurable timeout

## Fabric & NeoForge

- fix mob drops not being collected when Puzzles Lib defers spawning them ([**#97**](https://github.com/btwonion/magnetic/issues/97))
    - restores compatibility with Pick Up Notifier on Fabric
- fix a memory leak that was caused by the animation not untracking the item entities after completion

## Paper

- make Magnetic enchantment name translatable
  - the client mod will now actually translate the enchantment name :)
- add compatibility for the [TreeCapitator datapack](https://modrinth.com/datapack/treecapitator) ([**#89**](https://github.com/btwonion/magnetic/pull/89))