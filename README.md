# Magnetic (Telekinesis for Minecraft)

Pick up drops and XP instantly. No running around, no missed loot.
Inspired by the Hypixel Skyblock Telekinesis enchantment.

---

## Demo

![Magnetic auto-pickup demo](https://raw.githubusercontent.com/btwonion/magnetic/refs/heads/master/media/magnetic-demo-cave.gif)

---

## Why this mod exists

**Collecting drops is busywork.** Magnetic removes the cleanup loop so you can stay focused on building, mining, and combat.

---

## Highlights

Magnetic is designed to feel **instant**, **lightweight**, and **configurable** without getting in your way.

- **Auto-pickup:** Items and XP go straight to you.
- **Optional animation:** Items fly to you instead of popping in.
- **Rule-based activation:** Enchantment, sneak, or permission conditions.
- **Server-friendly:** Lightweight and low overhead.

![Mining comparison (without Magnetic, then with Magnetic)](https://raw.githubusercontent.com/btwonion/magnetic/refs/heads/master/media/magnetic-mining-compare.gif)

---

## Compatibility

**Works with:**
- **Fabric:** FallingTree, KleeSlabs, RightClickHarvest, TreeHarvester, Veinminer
- **Paper:** mcMMO, AuraSkills, GravesX

---

## Usage

**Equip a Magnetic-enchanted tool**, break or kill something, and **watch drops fly to you**.
Prefer always-on? Remove `ENCHANTMENT` from the condition statement.

---

## Commands and Permissions

**Commands:**
- **`/magnetic reload`** (OP/level 3) reloads the config

**Permission gate:**
- **`magnetic.ability.use`**

---

## Configuration

**Config file:** `magnetic.json`

Minimal example:

```json5
{
  "config": {
    "conditionStatement": {
      "raw": "ENCHANTMENT"
    },
    "itemsAllowed": true,
    "expAllowed": true,
    "animation": {
      "enabled": true
    }
  }
}
```

Apply changes with **`/magnetic reload`** or use the **config screen** on the client.
**Full reference:** [`docs/CONFIG.md`](https://github.com/btwonion/magnetic/blob/master/docs/CONFIG.md)

**Condition examples:**
- **`ENCHANTMENT`** (default)
- **`ENCHANTMENT || PERMISSION`**
- **`SNEAK`**

---

## FAQ

**Does this work in multiplayer?**  
Yes — **servers and singleplayer** are supported.

**Where do I get the enchantment?**  
It’s **vanilla-style**: trading, loot, or the enchantment table.

**Do I have to use the enchantment?**  
No. Remove `ENCHANTMENT` from the condition statement.

**Can I restrict Magnetic to permissions only?**  
Yes. Use `PERMISSION` in the condition statement and grant **`magnetic.ability.use`**.

**Can I disable item or XP pickup separately?**  
Yes. Toggle **`itemsAllowed`** and **`expAllowed`** in the config.

**My inventory is full — what happens?**  
Magnetic can play a **sound**, **message**, or **title** alert. Configure cooldowns in **`fullInventoryAlert`**.

**Can other players steal the floating items?**  
Optional. Set **`animation.canOtherPlayersPickup`** to control interception.

---

## Support

- **Issues:** https://github.com/btwonion/magnetic/issues
- **Discord:** https://nyon.dev/discord
