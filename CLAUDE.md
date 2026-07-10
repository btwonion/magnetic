# CLAUDE.md — Telekinesis / Magnetic

## Project Overview

**Magnetic** is a Minecraft project for Fabric/Quilt, staged NeoForge, and Paper that adds a "Magnetic" enchantment (formerly "Telekinesis"). When a player has the enchantment, item drops and XP fly directly into their inventory with an optional animation. The mod ID is `magnetic`.

## Project Structure

```text
mod/                         # Stonecutter + ModStitch multiloader mod
  src/main/java/             # Shared Java mixins
  src/main/kotlin/           # Shared/preprocessed Kotlin logic
  src/main/resources/        # Shared runtime resources and class tweaker
  src/main/templates/        # Fabric and NeoForge metadata templates
  versions/26.2-fabric/      # Active Fabric version node
paper/                       # Independent Paper plugin
docs/                        # CONFIG.md documentation
```

Loader-specific branches in shared mod sources use Stonecutter directives. Fabric is the only active version node; NeoForge source and metadata are staged, but no NeoForge node is configured or built yet.

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

| Mixin                           | Hook                        | Covers                                                                                |
|---------------------------------|-----------------------------|---------------------------------------------------------------------------------------|
| `ServerPlayerGameModeMixin`     | `destroyBlock`, `useItemOn` | Block breaking, item interactions (berries, beehives, pumpkins, etc.)                 |
| `LevelMixin`                    | `destroyBlock`              | Chain-broken blocks (falls back to PositionTracker lookup)                            |
| `LivingEntityMixin`             | `dropAllDeathLoot`          | Mob death drops                                                                       |
| `ServerGamePacketListenerMixin` | `handleInteract`            | Entity interactions                                                                   |
| `FishingHookMixin`              | `retrieve`                  | Fishing drops                                                                         |
| `BucketItemMixin`               | bucket use                  | Records placement in PositionTracker for fluid tracking                               |
| `FlowingFluidMixin`             | `spreadTo`                  | Water flow destroying blocks (looks up player from PositionTracker, sets threadLocal) |

### Mixin Target Class Rule

Mixin method references must target the class that **defines** the method, not a subclass. For example, `destroyBlock` is defined on `Level`, so `LevelMixin` targets `Level` — not `ServerLevel`.

### Conditional Compat Mixins

Fabric compat mixins have been removed. Paper still has compat event listeners (see below).

### Access Patterns

- `@Invoker` for private/protected methods (e.g., `ExperienceOrbInvoker` for `getValue()`/`setValue()`)
- Class tweaker (`magnetic.classtweaker`) for fields/methods needed at compile time

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

### Fabric
None (compat mixins removed in 26.1)

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

### Mod
- `mod/src/main/kotlin/dev/nyon/magnetic/Main.kt` — loader-branched entry point
- `mod/src/main/kotlin/dev/nyon/magnetic/Animation.kt` — item fly animation
- `mod/src/main/kotlin/dev/nyon/magnetic/DropEvent.kt` — event definition + handler
- `mod/src/main/kotlin/dev/nyon/magnetic/config/Config.kt` — config data class
- `mod/src/main/kotlin/dev/nyon/magnetic/config/conditions/` — condition system
- `mod/src/main/kotlin/dev/nyon/magnetic/utils/PositionTracker.kt` — block→player tracking
- `mod/src/main/kotlin/dev/nyon/magnetic/utils/MixinHelper.kt` — ThreadLocal + helpers
- `mod/src/main/java/dev/nyon/magnetic/mixins/ServerLevelMixin.java` — central interception
- `mod/src/main/resources/magnetic.mixins.json` — mixin registry

### Paper
- `paper/src/main/kotlin/dev/nyon/magnetic/Main.kt` — plugin entry point
- `paper/src/main/kotlin/dev/nyon/magnetic/listeners/` — all event handlers
- `paper/src/main/java/dev/nyon/magnetic/MagneticBootstrapper.java` — enchantment registration
- `paper/src/main/java/dev/nyon/magnetic/MagneticLoader.java` — dependency resolution
