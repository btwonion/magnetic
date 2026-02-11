package dev.nyon.magnetic;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PreFlattenTagRegistrar;
import io.papermc.paper.tag.TagEntry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class MagneticBootstrapper implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        LifecycleEventManager<@NotNull BootstrapContext> manager = context.getLifecycleManager();

        if (!needsEnchantment()) return;

        final TagKey<ItemType> TOOLS = TagKey.create(RegistryKey.ITEM, Key.key("magnetic:tools"));

        // Add tool tag
        manager.registerEventHandler(
            LifecycleEvents.TAGS.preFlatten(RegistryKey.ITEM), event -> {
                final PreFlattenTagRegistrar<ItemType> registrar = event.registrar();
                registrar.addToTag(
                    TOOLS, Set.of(
                            ItemTypeTagKeys.ENCHANTABLE_WEAPON,
                            ItemTypeTagKeys.ENCHANTABLE_MINING,
                            ItemTypeTagKeys.ENCHANTABLE_BOW,
                            ItemTypeTagKeys.ENCHANTABLE_CROSSBOW,
                            ItemTypeTagKeys.ENCHANTABLE_FISHING,
                            ItemTypeTagKeys.CREEPER_IGNITERS,
                            ItemTypeTagKeys.ENCHANTABLE_TRIDENT
                        )
                        .stream()
                        .map(TagEntry::tagEntry)
                        .collect(Collectors.toSet())
                );
            }
        );

        // Add enchantment
        final TypedKey<Enchantment> MAGNETIC = TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("magnetic:magnetic"));

        manager.registerEventHandler(RegistryEvents.ENCHANTMENT.compose()
            .newHandler(event -> event.registry()
                .register(
                    MAGNETIC,
                    builder -> builder.description(Component.text("Magnetic"))
                        .supportedItems(event.getOrCreateTag(TOOLS))
                        .weight(2)
                        .maxLevel(1)
                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(25, 0))
                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(75, 0))
                        .anvilCost(7)
                        .activeSlots(EquipmentSlotGroup.HAND)
                )));

        // Add enchantment to enchantment table, etc.
        manager.registerEventHandler(
            LifecycleEvents.TAGS.preFlatten(RegistryKey.ENCHANTMENT), event -> {
                final PreFlattenTagRegistrar<Enchantment> registrar = event.registrar();
                registrar.addToTag(EnchantmentTagKeys.TRADEABLE, Set.of(TagEntry.valueEntry(MAGNETIC)));
                registrar.addToTag(EnchantmentTagKeys.TREASURE, Set.of(TagEntry.valueEntry(MAGNETIC)));
                registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, Set.of(TagEntry.valueEntry(MAGNETIC)));
            }
        );
    }

    private boolean needsEnchantment() {
        try {
            List<String> configText = Files.readAllLines(Path.of("./plugins/magnetic/magnetic.json"));
            // Check for the enchantment option in the current config format
            if (configText.stream()
                .anyMatch(line -> line.contains("ENCHANTMENT"))) return true;
            // Check for the enchantment option in the past config formats
            return configText.stream()
                .anyMatch(line -> (line.contains("needEnchantment") || line.contains("enchantmentRequired")) &&
                    line.contains("true"));
        } catch (Exception e) {
            return true;
        }
    }
}
