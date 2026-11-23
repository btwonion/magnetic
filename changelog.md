## Fabric, NeoForge & Paper

- ignore the ender dragon by default, so its experience drops normally
  - only applies to new configs; to get this on an existing config, add `minecraft:ender_dragon` to `ignoreEntities`
- add compatibility with **Smelting Enchantment**, **Veinmine & Autosmelt Enchantments**, **Smelt Mining** and **Smelt Enchant**

## Fabric & NeoForge

- add compatibility with **Sophisticated Backpacks**, **FiltPick** and [**Server Tree Feller**](https://modrinth.com/mod/server-tree-feller)
- improve [**HT's TreeChop**](https://modrinth.com/mod/treechop) compatibility: every felled block is now checked against `ignoreBlocks`, not only the chopped log
- fix the pull animation on Fabric speeding up with every loaded dimension
- fix drops and experience of ignored entities being collected when they die during another player action, such as a melee hit on 1.21.x
- fix full inventory alerts showing raw translation keys when the mod is only installed on the server ([**#114**](https://github.com/btwonion/magnetic/pull/114))
- fix the Fabric version crashing on startup under **Sinytra Connector** ([**#77**](https://github.com/btwonion/magnetic/issues/77))

## Paper

- add compatibility with **AutoSmelt**, **ExcellentEnchants**, **SuperEnchants**, [**ThizzyZ Tree Feller**](https://modrinth.com/plugin/thizzyz-tree-feller) and [**TimberZ**](https://modrinth.com/plugin/timberz) ([**#62**](https://github.com/btwonion/magnetic/issues/62))
- fix **Veinminer** drops that only partly fit into the inventory being dropped in full, duplicating items
- fix the plugin failing to enable when a different plugin named "Veinminer" is installed
- fix several Folia issues, including bucket drops being handled on the wrong region thread and the pull animation speeding up with the number of active regions
- fix `buckets.abilityTimeout` being read as seconds instead of milliseconds
