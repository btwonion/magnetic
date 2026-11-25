![Magnetic: drops and XP go straight into your inventory](https://raw.githubusercontent.com/btwonion/magnetic/refs/heads/master/media/magnetic-banner.png)

**Mine, chop, farm, and fight without the cleanup.** Magnetic sends the drops and experience you cause directly to you. Mining above a lava lake, in a cave or in the Nether? The blocks and loot you break go into your inventory instead of falling into the lava and burning.

Use it as a survival enchantment, like Telekinesis from Hypixel SkyBlock, or switch it on for everyone with no enchantment needed. It works in singleplayer and on Fabric, NeoForge, and Paper servers.

## Features

- **Instant auto-pickup:** Collect block drops, mob loot, and XP without walking over them.
- **Not a magnet:** There is no pickup radius. Magnetic only collects the drops you cause, so it does not steal items from hoppers, farms, or other players.
- **Your choice of activation:** Use it as an enchantment, always on, only while sneaking, by permission, or any combination.
- **Satisfying or invisible:** Let drops fly toward you, change their speed, or turn off the animation for instant pickup.
- **Works with your other mods:** Drops from tree fellers, vein miners, harvest mods, and auto-smelt enchantments land in your inventory too, and backpacks with a pickup upgrade can catch them.
- **Precise exclusions:** Ignore drops from selected blocks or entities using Minecraft IDs or tags.
- **Bucket-aware pickup:** Collect drops caused by fluids from a bucket you placed.
- **Full-inventory alerts:** Choose a sound, chat message, or title, each with its own cooldown.

![Mining comparison: vanilla pickup followed by Magnetic](https://raw.githubusercontent.com/btwonion/magnetic/refs/heads/master/media/magnetic-mining-compare.gif)

## Choose how it works

By default, Magnetic is an enchantment you earn through the enchantment table, loot, and librarian trades. Put it on a tool, break a block or kill a mob, and the drops travel straight to you.

Prefer something else? Change one setting in `magnetic.json` or the in-game config screen:

| You want… | Setting |
| --- | --- |
| A survival enchantment to earn (default) | `conditionStatement.raw`: `ENCHANTMENT` |
| Always on, no enchantment | `conditionStatement.raw`: empty |
| Only while sneaking | `conditionStatement.raw`: `SNEAK` |
| Only for certain ranks or staff | `conditionStatement.raw`: `PERMISSION` |
| The enchantment for players, plus a rank bypass | `conditionStatement.raw`: `ENCHANTMENT \|\| PERMISSION` |
| XP only, items drop normally | `itemsAllowed`: `false` |
| Items only, XP drops normally | `expAllowed`: `false` |

## Works with

Drops from these mods and plugins go to your inventory as well:

- **Fabric and NeoForge:** FallingTree, HT's TreeChop, KleeSlabs, RightClickHarvest, Server Tree Feller, Tree Harvester, and Veinminer
- **Paper:** mcMMO, AuraSkills, ThizzyZ Tree Feller, TimberZ, [Veinminer by Miraculixx](https://modrinth.com/mod/veinminer), and the TreeCapitator datapack

**Auto-smelt:** Magnetic collects the smelted items of auto-smelt mods, plugins, and datapacks, including on blocks
mined with Veinminer. Loot-table datapacks such as Instant Smelt and mods such as Smelting Touch work as-is. For the
Smelting Enchantment, Veinmine & Autosmelt, Smelt Mining, and Smelt Enchant datapacks, drops land briefly so the
datapack can smelt them before Magnetic collects them. On Paper, Magnetic lets plugins such as AutoSmelt,
ExcellentEnchants, and SuperEnchants smelt drops first.

**Backpacks and pickup filters:** On Fabric and NeoForge, collected drops go through vanilla item pickup, so mods that
hook it apply to Magnetic's drops too. For example, Sophisticated Backpacks' pickup upgrade can take drops even when
your inventory is full, and pickup filters like FiltPick can leave unwanted items on the ground.

**Grave plugins on Paper:** add `minecraft:player` to `ignoreEntities` so a player's death drops go to their grave
instead of to the killer.

## For server owners

- **A cleaner world:** Players leave far fewer items lying around to despawn.
- **No client install on Paper:** Players can join with a vanilla client. Installing the client mod is optional.
- **Paper, Folia, and Purpur:** The plugin runs on all three.
- **Ranks and permissions:** Gate Magnetic behind `magnetic.ability.use`, or combine it with the enchantment.
- **Live reloads:** Apply config changes with `/magnetic reload` (OP/level 3).

## Install

Download the file for your platform from [Modrinth](https://modrinth.com/mod/magnetic/versions). Fabric and NeoForge builds are also available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/magnetic-telekinesis/files). Then add the required dependencies:

| Platform | Installation | Required dependencies |
| --- | --- | --- |
| Fabric | Put Magnetic in the `mods` folder | [Fabric API](https://modrinth.com/mod/fabric-api), [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin), and [YACL](https://modrinth.com/mod/yacl) |
| NeoForge | Put Magnetic in the `mods` folder | [KotlinLangForge](https://modrinth.com/mod/kotlin-lang-forge) and [YACL](https://modrinth.com/mod/yacl) |
| Paper, Folia, or Purpur | Put Magnetic in the `plugins` folder | None. Players can join without the mod; installing the client mod is recommended for the best experience. |

[Mod Menu](https://modrinth.com/mod/modmenu) is optional on Fabric and provides quick access to Magnetic's config screen.

## Configuration

Magnetic works out of the box with the enchantment. For more control, edit `magnetic.json` or use the in-game config screen:

| Setting | What it controls |
| --- | --- |
| `conditionStatement.raw` | When Magnetic activates; accepts `ENCHANTMENT`, `SNEAK`, `PERMISSION`, `&&`, and `\|\|` |
| `itemsAllowed` / `expAllowed` | Whether item drops and XP are collected |
| `buckets` | Whether and for how long placed-bucket effects trigger Magnetic |
| `leafDecay` | Whether natural leaf-decay drops caused by eligible log breaks are collected |
| `ignoreBlocks` / `ignoreEntities` | Source blocks and entities to exclude by ID or tag |
| `animation` | Flight effect, speed, and whether other players can intercept items |
| `fullInventoryAlert` | Sound, message, and title alerts with separate cooldowns |

Apply file changes with `/magnetic reload` (OP/level 3). The permission condition checks `magnetic.ability.use`.

See the [complete configuration reference](https://github.com/btwonion/magnetic/blob/master/docs/CONFIG.md) for every setting and its default.

## FAQ

### Where can I find the Magnetic enchantment?

Magnetic is available through the enchantment table, loot, and librarian trades. With the Villager Trade Rebalance experiment disabled, librarians from any biome can offer it. With the experiment enabled, look for a desert librarian; Magnetic is part of the desert's common enchanted-book pool.

### Do I have to use the enchantment?

No. Leave the condition statement empty to make Magnetic always active, or use `SNEAK` or `PERMISSION` instead. See [Choose how it works](#choose-how-it-works).

### Can I turn it off for a moment, for example while digging out a room?

Set the condition statement to `SNEAK` or `ENCHANTMENT && SNEAK`. Magnetic then only collects drops while you crouch.

### Can I collect only XP?

Yes. Set `itemsAllowed` to `false`, and items drop as usual while experience goes straight to you.

### Does Magnetic work in multiplayer?

Yes. It supports singleplayer, Fabric and NeoForge servers, and Paper, Folia, and Purpur servers. Players joining a Paper server do not need to install Magnetic themselves.

### What happens when my inventory is full?

Drops stay safe, and Magnetic can notify you with a sound, chat message, or title.

### Do collected items count toward my statistics?

On Fabric and NeoForge, yes. Items Magnetic collects count as picked up, including stacks that only partly fit into a full inventory.

### Why does the ender dragon's XP still drop?

The ender dragon is in `ignoreEntities` by default, so its experience drops normally at the end portal. Remove it from the list to collect the XP instead. Config files created before this default keep their existing list.

### Can other players take items while they are flying toward me?

You decide. Set `animation.canOtherPlayersPickup` to control whether flying items can be intercepted.

### Is this the old Telekinesis mod?

Yes. Magnetic was called Telekinesis before version 3.2.0. The mod and the enchantment were renamed, but the feature is the same.

## Support

If you need help with any of my mods, just join my [discord server](https://nyon.dev/discord).
