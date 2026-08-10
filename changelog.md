## Fabric, NeoForge & Paper

- add optional collection of natural leaf-decay drops ([**#99**](https://github.com/btwonion/magnetic/pull/99))
    - disabled by default through the new `leafDecay` config section
    - preserves eligibility from the original log break and supports accelerated decay mods
    - expires player attribution after a configurable timeout

## Fabric

- fix mob drops not being collected when Puzzles Lib defers spawning them ([**#97**](https://github.com/btwonion/magnetic/issues/97))
    - restores compatibility with Pick Up Notifier on Fabric
