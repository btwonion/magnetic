# Magnetic

> Telekinesis for Minecraft: drops and XP go straight into your inventory.

**Mine, chop, and fight without the cleanup.** Magnetic sends item drops and experience directly to you, so loot cannot get left in tall grass or scattered across a cave.

Inspired by the Hypixel SkyBlock Telekinesis enchantment, Magnetic feels like a natural part of survival: obtain the enchantment through normal gameplay, put it on a tool, and keep doing what you were doing.

![Magnetic auto-pickup demo](https://raw.githubusercontent.com/btwonion/magnetic/refs/heads/master/media/magnetic-demo-cave.gif)

## Features

- **Instant auto-pickup:** Collect item drops and XP without walking over them.
- **Satisfying or invisible:** Let drops fly toward you, change their speed, or disable the animation for instant pickup.
- **Vanilla-style progression:** Find Magnetic through trading, loot, or the enchantment table.
- **Flexible activation:** Require the enchantment, sneaking, a permission, or a combination of conditions.
- **Precise exclusions:** Ignore drops from selected blocks or entities using Minecraft IDs or tags.
- **Bucket-aware pickup:** Collect drops caused by fluids from a bucket you placed.
- **Full-inventory alerts:** Choose a sound, chat message, or title, each with its own cooldown.
- **Multiplayer-ready:** Use the same core behavior in singleplayer or on Fabric, NeoForge, and Paper servers.

![Mining comparison: vanilla pickup followed by Magnetic](https://raw.githubusercontent.com/btwonion/magnetic/refs/heads/master/media/magnetic-mining-compare.gif)

## How it works

1. Equip a tool with the **Magnetic** enchantment.
2. Break a block or defeat a mob.
3. Its drops and XP travel straight to you.

That is the default behavior. Server owners and modpack authors can also make Magnetic always active, permission-based, or active only while a player sneaks.

## Install

Download the file for your platform from [Modrinth](https://modrinth.com/mod/magnetic/versions). Fabric and NeoForge builds are also available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/magnetic-telekinesis/files). Then add the required dependencies:

| Platform | Installation | Required dependencies                                                                                                                                                  |
| --- | --- |------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Fabric | Put Magnetic in the `mods` folder | [Fabric API](https://modrinth.com/mod/fabric-api), [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin), and [YACL](https://modrinth.com/mod/yacl) |
| NeoForge | Put Magnetic in the `mods` folder | [KotlinLangForge](https://modrinth.com/mod/kotlin-lang-forge) and [YACL](https://modrinth.com/mod/yacl)                                                                |
| Paper | Put Magnetic in the `plugins` folder | None; players can join without installing the mod, but for the best experience the client mod is required as well                                                      |

[Mod Menu](https://modrinth.com/mod/modmenu) is optional on Fabric and provides quick access to Magnetic's config screen.

## Compatibility

**Minecraft versions:** Fabric and NeoForge builds are available for 1.21.1, 1.21.11, 26.1.1–26.1.2, and 26.2. Paper builds are published alongside the mod from the same project.

**Tested integrations:**

- **Fabric and NeoForge:** FallingTree, KleeSlabs, RightClickHarvest, Tree Harvester, and Veinminer
- **Paper:** mcMMO, AuraSkills, and GravesX

## Configuration

Magnetic works out of the box with the enchantment. For more control, edit `magnetic.json` or use the in-game config screen:

| Setting | What it controls |
| --- | --- |
| `conditionStatement.raw` | When Magnetic activates; accepts `ENCHANTMENT`, `SNEAK`, `PERMISSION`, `&&`, and `\|\|` |
| `itemsAllowed` / `expAllowed` | Whether item drops and XP are collected |
| `buckets` | Whether and for how long placed-bucket effects trigger Magnetic |
| `ignoreBlocks` / `ignoreEntities` | Source blocks and entities to exclude by ID or tag |
| `animation` | Flight effect, speed, and whether other players can intercept items |
| `fullInventoryAlert` | Sound, message, and title alerts with separate cooldowns |

Common condition statements:

- `ENCHANTMENT` — the default
- `ENCHANTMENT || PERMISSION` — enchantment for players, permission override for staff or ranks
- `SNEAK` — active while crouching
- An empty statement — always active

Apply file changes with `/magnetic reload` (OP/level 3). The permission condition checks `magnetic.ability.use`.

See the [complete configuration reference](https://github.com/btwonion/magnetic/blob/master/docs/CONFIG.md) for every setting and its default.

## FAQ

### Does Magnetic work in multiplayer?

Yes. It supports singleplayer, modded servers, and Paper servers. Players joining a Paper server do not need to install Magnetic themselves.

### Do I have to use the enchantment?

No. Change the condition statement to make Magnetic always active or use `SNEAK` or `PERMISSION` instead.

### What happens when my inventory is full?

Drops remain safe, and Magnetic can notify you with a sound, chat message, or title.

### Can other players take items while they are flying toward me?

You decide. Set `animation.canOtherPlayersPickup` to control whether flying items can be intercepted.

## Support

- **Issues:** https://github.com/btwonion/magnetic/issues
- **Discord:** https://nyon.dev/discord
