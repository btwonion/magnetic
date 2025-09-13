# Magnetic (Telekinesis for Minecraft)

**Magnetically move items and experience directly into your inventory!
Inspired by the Hypixel Skyblock Telekinesis enchantment.**  
No more running around to collect drops—just break, kill, or mine, and let the loot fly to you!

---

## ✨ Features

- 🧲 **Automatic Pickup**: Items and experience orbs zip straight into your inventory.
- ⚙️ **Configurable**: Fine-tune exactly how magnetic works for you.
- 🚀 **Performance-Friendly**: Lightweight, designed for both servers and single-player.
- 🔄 **Flexible**: Easy toggling and customizability.

---

## 🎬 Demo

![Block drop auto-pickup preview](https://raw.githubusercontent.com/btwonion/magnetic/refs/heads/master/media/magnetic-showcase-cave.gif)

*Watch items and XP zip right to you!*

---

## ❓ FAQ

- **Does this work in multiplayer?**  
  Yes, both on servers and singleplayer!

- **Is it compatible with other mods/plugins?**  
  Designed for broad compatibility, but let us know if you find issues!

- **Where can I find the enchantment?**
  The enchantment can be found like any vanilla enchantment. By trading, in treasures or via the enchantment table.

- **Do I need to use the enchantment?**
  No, just toggle the `needEnchantment` option! Like this, the enchantment will be removed completely.

- **Can I configure the mod/plugin to only work on people that have a certain permission?**
  Yes, you can toggle the `needPermission` option!
  If you disable `needEnchantment` as well, the permission will function as a drop-in replacement for the enchantment

---

## ⚙️ Configuration

Configuration is handled via the `magnetic.json` file:

```json5
{
    "version": 2, // For migration purposes only, just ignore this.
    "config": {
        "enchantmentRequired": true, // The magnetic ability will only work if the tool in the off- or main-hand is enchanted with magnetic. If this option is set to false, the enchantment will not be available.
        "sneakRequired": false, // Requires the player to sneak to use magnetic.
        "permissionRequired": false, // Requires the player to have a certain permission ('magnetic.ability.use') to use magnetic.
        "itemsAllowed": true, // Allows the player to also pickup items with magnetic.
        "expAllowed": true, // Allows the player to also pickup exp with magnetic.
        "ignoreRangedWeapons": true, // Ignores drops that were produced via a kill with a long-ranged weapon (e.g. bow, crossbow, etc.).
        "ignoreEntities": [] // The specified entities will not be affected by magnetic when killed. You can use both tags and entity ids (resource locations) to define which entities to ignore.
    }
}
```

Changes require a server or game restart to take effect.

---

## 💬 Support & Feedback

- Open an [issue](https://github.com/btwonion/magnetic/issues) for bugs or suggestions.
- Join our [Discord](https://nyon.dev/discord) for help and community.
