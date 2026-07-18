# NeoForge Access Transformer Fix

## Problem

NeoForge fails during mod discovery because Modstitch 0.8.4 converts the shared
class-tweaker field entry

```text
accessible field net/minecraft/world/entity/player/Inventory items Lnet/minecraft/core/NonNullList;
```

into this access-transformer directive:

```text
public net.minecraft.world.entity.player.Inventory items Lnet/minecraft/core/NonNullList;
```

NeoForge access-transformer field directives accept a modifier, class name, and
field name, but no field descriptor. The extra descriptor therefore causes the
access-transformer parser to abort startup.

## Root Cause

The `Inventory.items` entry was originally needed by code that accessed the
field directly. That code has since been removed, while the class-tweaker entry
remained. No current Java or Kotlin source accesses `Inventory.items`.

The malformed NeoForge output is therefore caused by an obsolete access request,
not a current runtime requirement.

## Design

Remove the obsolete `Inventory.items` entry from
`mod/src/main/resources/magnetic.classtweaker`. Keep the
`RegistryLoadTask$PendingRegistration` class entry because current mixin code
uses that otherwise-inaccessible nested class.

Do not introduce loader-specific class-tweaker files, generated-file
post-processing, or a local Modstitch patch. Those approaches would retain an
unused transformation and add build-system complexity.

## Regression Protection

Add a build-level verification that examines generated NeoForge access
transformers and rejects field directives with extra tokens. The check must fail
against the current generated output before the source entry is removed and pass
afterward.

The check should operate on generated packaging output rather than merely
scanning the source class-tweaker file. This verifies the format boundary that
actually failed at runtime.

## Verification

1. Run the regression check before the fix and confirm it identifies the
   malformed `Inventory.items` directive.
2. Remove the obsolete class-tweaker entry.
3. Run the regression check again and confirm it passes.
4. Build every supported NeoForge target.
5. Inspect each final NeoForge JAR and confirm its
   `META-INF/accesstransformer.cfg` contains only syntactically valid directives.
6. Run the broader project build to detect cross-loader regressions.

## Scope

This change fixes NeoForge packaging only. It does not change gameplay behavior,
mixins, mappings, dependencies, or supported Minecraft versions.
