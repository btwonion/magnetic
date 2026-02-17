# Magnetic config reference

This file documents every field in `magnetic.json` and their defaults.

Config file structure:

```json5
{
  "version": 4,
  "config": {
    "conditionStatement": {
      "raw": "ENCHANTMENT"
    },
    "itemsAllowed": true,
    "expAllowed": true,
    "ignoredEntitiesRangeMin": 50.0,
    "ignoreEntities": [],
    "fullInventoryAlert": {
      "soundAlert": {
        "enabled": true,
        "cooldownInSeconds": 5
      },
      "textAlert": {
        "enabled": true,
        "cooldownInSeconds": 60
      },
      "titleAlert": {
        "enabled": false,
        "cooldownInSeconds": 5
      }
    },
    "animation": {
      "enabled": true,
      "blocksPerSecond": 1.0,
      "canOtherPlayersPickup": false
    }
  }
}
```

Notes:
- `version` is used for config migrations.
- All fields live inside the `config` object.

## conditionStatement.raw
Defines when Magnetic is active. The statement is evaluated left-to-right.

Operators:
- `AND` or `&&`
- `OR` or `||`

Conditions:
- `ENCHANTMENT` - player holds a Magnetic-enchanted item
- `SNEAK` - player is crouching
- `PERMISSION` - player has `magnetic.ability.use`

Examples:
- `ENCHANTMENT`
- `ENCHANTMENT || PERMISSION`
- `SNEAK && PERMISSION`

## itemsAllowed
If `true`, items are pulled into the inventory.

## expAllowed
If `true`, XP orbs are pulled into the inventory.

## ignoredEntitiesRangeMin
Ignores drops if the player was farther than this distance from the entity.
- Set to `-1` to disable the range check.

## ignoreEntities
List of entities to ignore entirely.
- Accepts entity ids (resource locations) and tags.
Examples:
- `minecraft:creeper`
- `#minecraft:skeletons`

## fullInventoryAlert
Alerts when Magnetic tries to insert items but the inventory is full.

### fullInventoryAlert.soundAlert
- `enabled`: play a sound when inventory is full
- `cooldownInSeconds`: minimum time between alerts

### fullInventoryAlert.textAlert
- `enabled`: send a chat message when inventory is full
- `cooldownInSeconds`: minimum time between alerts

### fullInventoryAlert.titleAlert
- `enabled`: show a title when inventory is full
- `cooldownInSeconds`: minimum time between alerts

## animation
Controls the optional "items fly to you" effect.

### animation.enabled
If `true`, items travel toward the player instead of instant pickup.

### animation.blocksPerSecond
Movement speed of items while flying.

### animation.canOtherPlayersPickup
If `true`, other players can intercept flying items.
