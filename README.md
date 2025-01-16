# magnetic (pka. telekinesis)

> Magnetically moves items and experience into your inventory. Also known as telekinesis from Hypixel Skyblock.

## Functionality

**Block drop auto-pickup preview**

![Block drop auto-pickup preview](https://raw.githubusercontent.com/btwonion/magnetic/refs/heads/master/media/magnetic-showcase-cave.gif)

With this mod you can automatically pick up drops, including experience, from mobs, blocks and other entities.

## Can I use the functionality without the enchantment?

Yes, in the [config](#Configuration) you can change the `needEnchantment` option to `false`. If you want to only apply
this functionality while sneaking, you can also enable `needSneak`.

Nevertheless, if you play with the enchantment enabled, you will have to hold a magnetic-enchanted tool in your main- or
offhand.

## Where can I find the enchantment?

You can trade the enchantment with villagers, enchant it in an enchanting table or you can find it in treasures
enchanted on tools.

## Configuration

The configuration file can be found in the client/server directory.

-> `/config/magnetic.json` (fabric) 

-> `/plugins/magnetic.json` (paper) 

<details>
<summary>magnetic.json</summary>

```json5
{
    "version": 1,
    // For migration purposes only, just ignore this.
    "config": {
        "needEnchantment": true,
        // Defines whether Magnetic should without or with the enchantment on the tool.
        "needSneak": false,
        // Defines whether the player should have to sneak to use Magnetic.
        "expAllowed": true,
        // Enables the use of Magnetic for exp drops.
        "itemsAllowed": true
        // Enables the use of Magnetic for item drops.
    }
}
```

</details>

### Other

Only the latest stable version of Minecraft will have feature updates. From 1.21.4 on, there will be only bug fixes to
older versions of Minecraft.

If you need help with any of my mods, join my [discord server](https://nyon.dev/discord).
