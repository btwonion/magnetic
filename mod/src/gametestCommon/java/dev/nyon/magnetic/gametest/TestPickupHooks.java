package dev.nyon.magnetic.gametest;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Stands in for third-party mods that hook vanilla item pickup, such as backpack pickup upgrades and pickup
 * filters. Fabric calls it from a mixin at the head of {@code ItemEntity.playerTouch}; NeoForge calls it from
 * {@code ItemEntityPickupEvent.Pre}. Both are the hooks those mods use on each loader.
 */
public final class TestPickupHooks {
    private static volatile Hook hook;

    private TestPickupHooks() {
    }

    @FunctionalInterface
    public interface Hook {
        /** @return whether vanilla pickup must be cancelled */
        boolean handle(ItemEntity itemEntity, Player player);
    }

    public static boolean handle(ItemEntity itemEntity, Player player) {
        Hook current = hook;
        return current != null && current.handle(itemEntity, player);
    }

    public static void run(Hook installed, Runnable action) {
        hook = installed;
        try {
            action.run();
        } finally {
            hook = null;
        }
    }
}
