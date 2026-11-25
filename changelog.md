## Fabric, NeoForge & Paper

- ignore the ender dragon by default, so its experience drops normally
  - only applies to new configs; to get this on an existing config, add `minecraft:ender_dragon` to `ignoreEntities`
- add compatibility with **Smelting Enchantment**, **Veinmine & Autosmelt Enchantments**, **Smelt Mining** and **Smelt Enchant**
- make `animation.blocksPerSecond` follow the server tick rate set with `/tick rate`, and apply it after `/magnetic reload` instead of only after a restart
- report an invalid `conditionStatement` to each player at most once a minute instead of on every drop

## Fabric & NeoForge

- support Minecraft **26.4-pre-1** on Fabric
- add compatibility with **Sophisticated Backpacks**, **FiltPick** and [**Server Tree Feller**](https://modrinth.com/mod/server-tree-feller)
- improve [**HT's TreeChop**](https://modrinth.com/mod/treechop) compatibility: every felled block is now checked against `ignoreBlocks`, not only the chopped log
- fix the pull animation on Fabric speeding up with every loaded dimension
- fix drops and experience of ignored entities being collected when they die during another player action, such as a melee hit on 1.21.x
- fix full inventory alerts showing raw translation keys when the mod is only installed on the server ([**#114**](https://github.com/btwonion/magnetic/pull/114))
- fix the Fabric version crashing on startup under **Sinytra Connector** ([**#77**](https://github.com/btwonion/magnetic/issues/77))
- fix "Reset to default" in the config screen restoring wrong values for `expAllowed`, `itemsAllowed`, `ignoredEntitiesRangeMin` and the title alert

## Paper

- support Minecraft **1.21 – 1.21.11** alongside 26.1 – 26.3
- collect every block that breaks with the block you mine, such as torches, flowers, carpets, signs and banners on top of it, as well as whole **big dripleaf** plants and **pointed dripstone** stalagmites
  - these blocks now drop exactly what vanilla drops; the tool no longer applies to them, so shears no longer collect every vine of a column
- add compatibility with **AutoSmelt**, **ExcellentEnchants**, **SuperEnchants**, [**ThizzyZ Tree Feller**](https://modrinth.com/plugin/thizzyz-tree-feller) and [**TimberZ**](https://modrinth.com/plugin/timberz) ([**#62**](https://github.com/btwonion/magnetic/issues/62))
- fix **Veinminer** drops that only partly fit into the inventory being dropped in full, duplicating items
- fix the plugin failing to enable when a different plugin named "Veinminer" is installed
- fix several Folia issues, including bucket drops being handled on the wrong region thread and the pull animation speeding up with the number of active regions
- fix `buckets.abilityTimeout` being read as seconds instead of milliseconds
- fix a misspelled condition in `conditionStatement` enabling Magnetic for every player; invalid statements now disable it and are reported, as on Fabric and NeoForge
- fix full inventory alert settings not applying after `/magnetic reload`
- fix blank lines and values containing `=` in translation files breaking or truncating translations
- fix the Magnetic enchantment being registered or skipped based on the wrong config file when the server uses a custom plugins folder
