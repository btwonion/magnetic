# CLAUDE.md — Telekinesis / Magnetic

## Project Overview

**Magnetic** is a dual-platform Minecraft mod (Fabric + Paper) that adds a "Magnetic" enchantment (formerly "Telekinesis"). When a player has the enchantment, item drops and XP fly directly into their inventory with an optional animation. The mod ID is `magnetic`.

## Project Structure

```
fabric/          # Fabric mod (Mixin-based)
  src/main/java/   dev/nyon/magnetic/mixins/     # Mixin classes (Java)
  src/main/kotlin/ dev/nyon/magnetic/             # Mod logic (Kotlin)
  src/main/resources/                             # Mixin configs, fabric.mod.json, access widener
paper/           # Paper plugin (Bukkit event-based)
  src/main/java/   dev/nyon/magnetic/             # Bootstrapper, Loader (Java)
  src/main/kotlin/ dev/nyon/magnetic/             # Plugin logic (Kotlin)
docs/            # CONFIG.md documentation
```

## Fabric Mixin Architecture

### ThreadLocal Player Tracking

`MixinHelper.threadLocal` (a `ThreadLocal<ServerPlayer>`) is the core mechanism for associating drops with the player who caused them. Mixins set it before an operation and clear it after:

```java
MixinHelper.threadLocal.set(player);  // HEAD inject
// ... operation runs, drops spawn ...
MixinHelper.threadLocal.remove();     // RETURN inject — always use .remove(), never .set(null)
```

### Central Entity Interception

`ServerLevelMixin.addFreshEntity()` is the single interception point. When an `ItemEntity` or `ExperienceOrb` is about to spawn, it looks up the responsible player from `threadLocal` or `PositionTracker`, fires a `DropEvent`, and cancels the entity spawn if the drop is collected.

### PositionTracker

`PositionTracker` (per-`ServerLevel` via `ServerLevelHolder` interface) maps `BlockPos → (ServerPlayer, timestamp)` for chain-propagation scenarios (e.g., breaking bamboo causes upper blocks to break in separate ticks). Entries expire after 5 seconds. Cleaned up each tick.

### Player-Setting Mixins

Each mixin follows the same pattern — set threadLocal at HEAD, remove at RETURN:

| Mixin | Hook | Covers |
|-------|------|--------|
| `ServerPlayerGameModeMixin` | `destroyBlock`, `useItemOn` | Block breaking, item interactions (berries, beehives, pumpkins, etc.) |
| `LevelMixin` | `destroyBlock` | Chain-broken blocks (falls back to PositionTracker lookup) |
| `LivingEntityMixin` | `dropAllDeathLoot` | Mob death drops |
| `ServerGamePacketListenerMixin` | `handleInteract` | Entity interactions |
| `FishingHookMixin` | `retrieve` | Fishing drops |
| `BucketItemMixin` | bucket use | Records placement in PositionTracker for fluid tracking |

### Mixin Target Class Rule

Mixin method references must target the class that **defines** the method, not a subclass. For example, `destroyBlock` is defined on `Level`, so `LevelMixin` targets `Level` — not `ServerLevel`.

### Conditional Compat Mixins

Each compatibility mod has its own `IMixinConfigPlugin` that checks `FabricLoader.getInstance().isModLoaded("modid")` in `shouldApplyMixin()`. Compat mixin configs are separate JSON files (e.g., `compat.veinminer.mixins.json`).

### Access Patterns

- `@Invoker` for private/protected methods (e.g., `ExperienceOrbInvoker` for `getValue()`/`setValue()`)
- Access widener (`magnetic.accesswidener`) for fields/methods needed at compile time

## Configuration

Config file: `magnetic.json` (Fabric: config dir, Paper: `plugins/magnetic/`)

Uses `konfig` library (from `repo.nyon.dev`) with version-based migration.

### Condition System

Conditions are parsed from a string expression like `"ENCHANTMENT && SNEAK || PERMISSION"` (left-to-right evaluation, no operator precedence).

Three condition types:
- **ENCHANTMENT** — player has the magnetic enchantment on held item
- **SNEAK** — player is crouching
- **PERMISSION** — player has `magnetic.ability.use` permission

## Compatibility Mods

### Fabric (via conditional mixins)
FallingTree, KleeSlabs, RightClickHarvest, TreeHarvester, Veinminer

### Paper (via event listeners)
mcMMO, AuraSkills, Veinminer

## Testing

After implementing a feature or fix, ask the user to test it in-game rather than assuming it works. Do not run build or test commands yourself.

## Code Conventions

- **Kotlin** for all mod logic, config, events, extensions
- **Java** for mixins, invokers, mixin plugins, and Paper bootstrap/loader classes
- ThreadLocal cleanup: always `threadLocal.remove()`, never `threadLocal.set(null)`
- Coroutine-based animation with `Mutex` for thread safety
- Paper uses `Entity.scheduler` / Bukkit scheduler; Fabric uses server tick events
- Custom `DropEvent` on both platforms with signature: `(MutableList<ItemStack>, MutableInt, Player, BlockPos/Location)`

## Key Files

### Fabric
- `fabric/src/main/kotlin/dev/nyon/magnetic/Main.kt` — entry point
- `fabric/src/main/kotlin/dev/nyon/magnetic/Animation.kt` — item fly animation
- `fabric/src/main/kotlin/dev/nyon/magnetic/DropEvent.kt` — event definition + handler
- `fabric/src/main/kotlin/dev/nyon/magnetic/config/Config.kt` — config data class
- `fabric/src/main/kotlin/dev/nyon/magnetic/config/conditions/` — condition system
- `fabric/src/main/kotlin/dev/nyon/magnetic/utils/PositionTracker.kt` — block→player tracking
- `fabric/src/main/java/dev/nyon/magnetic/utils/MixinHelper.java` — ThreadLocal + helpers
- `fabric/src/main/java/dev/nyon/magnetic/mixins/ServerLevelMixin.java` — central interception
- `fabric/src/main/resources/magnetic.mixins.json` — mixin registry

### Paper
- `paper/src/main/kotlin/dev/nyon/magnetic/Main.kt` — plugin entry point
- `paper/src/main/kotlin/dev/nyon/magnetic/listeners/` — all event handlers
- `paper/src/main/java/dev/nyon/magnetic/MagneticBootstrapper.java` — enchantment registration
- `paper/src/main/java/dev/nyon/magnetic/MagneticLoader.java` — dependency resolution
