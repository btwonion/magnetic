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
  No, just change the condition statement to exclude the `ENCHANTMENT` condition!
  Like this, the enchantment will be removed completely.

- **Can I configure the mod/plugin to only work on people that have a certain permission?**
  Yes, you can add the `PERMISSION` to the condition statement. For example: `ENCHANTMENT || PERMISSION` will work for
  players that have the enchantment or the required permission.
  The permission to check for is `magnetic.ability.use`.

---

## ⚙️ Configuration

Configuration is handled via the `magnetic.json` file and can be edited as well in the config screen on the client:

```json5
{
    "version": 4,
    // For migration purposes only, just ignore this.
    "config": {
        "conditionStatement": {
            "raw": "ENCHANTMENT"
            // Sets the conditions that are required for magnetic to work. The format is a logical operation that processes from start to end of the text and accepts the following statements: Operators: AND (&&), OR (||), Conditions: ENCHANTMENT, SNEAK, PERMISSIONIf the text is empty, no check will be applied.
        },
        "itemsAllowed": true, // Allows the player to also pick up items with magnetic.
        "expAllowed": true, // Allows the player to also pick up exp with magnetic.
        "ignoredEntitiesRangeMin": 15.0, // Ignores drops that were produced by a player that was further away from the entity than this value. If this value is set to -1, no check will be performed.
        "ignoreEntities": [], // Magnetic will not affect the specified entities when killed. You can use both tags and entity ids (resource locations) to define which entities to ignore.
        "fullInventoryAlert": {
            "soundAlert": {
                "enabled": true, // Enables sound alerts that trigger when magnetic tries to add an item to the inventory, but the inventory is already full.
                "cooldownInSeconds": 5 // The time that has to pass to play the sound alert again.
            },
            "textAlert": {
                "enabled": true, // Enables text message alerts that trigger when magnetic tries to add an item to the inventory, but the inventory is already full.
                "cooldownInSeconds": 60 // The time that has to pass to show the message alert again.
            },
            "titleAlert": {
                "enabled": true, // Enables title alerts that trigger when magnetic tries to add an item to the inventory, but the inventory is already full.
                "cooldownInSeconds": 5 // The time that has to pass to show the title alert again.
            }
        },
        "animation": {
            "enabled": true, // When enabled, all the items that are handled by magnetic will be pulled towards you instead of directly being put in your inventory.
            "blocksPerSecond": 1.0, // Defines how fast the items should be pulled towards a player.
            "canOtherPlayersPickup": false // Toggles whether other players can intercept the floating items and pick them up.
        }
    }
}
```

Changes require a server or game restart to take effect.

---

## 💬 Support & Feedback

- Open an [issue](https://github.com/btwonion/magnetic/issues) for bugs or suggestions.
- Join our [Discord](https://nyon.dev/discord) for help and community.
