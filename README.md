# telekinesis

This mod/plugin adds the telekinesis enchantment, allowing you to instantly move exp and item drops into your inventory.
This includes the drops from mobs, vehicles and blocks.

## Where can I get this enchantment?

You can trade this enchantment with villagers, enchant it in an enchanting table, or you can find it in treasures
enchanting on tools.

## Configuration

The configuration file can be found in the client/server directory.
-> configuration file: `/config/telekinesis.json`

<details>
<summary>telekinesis.json</summary>

```json5
{
    "version": 1,
    // For migration purposes only, just ignore this.
    "config": {
        "needEnchantment": true,
        // Defines, whether telekinesis should without or with the enchantment on the tool.
        "needSneak": false,
        // Defines. whether the player should have to sneak in order to use telekinesis.
        "expAllowed": true,
        // Enables the use of telekinesis for exp drops.
        "itemsAllowed": true
        // Enables the use of telekinesis for item drops.
    }
}
```

</details>

### Other

If you need help with any of my mods, just join my [discord server](https://nyon.dev/discord).

#### Paper Compatibility

The paper module of telekinesis is as of Minecraft version 1.20.2 discontinued cause of the lack of ability to
register the enchantment.
