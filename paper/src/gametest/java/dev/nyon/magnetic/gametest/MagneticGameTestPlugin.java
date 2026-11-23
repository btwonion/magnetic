package dev.nyon.magnetic.gametest;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Runs Magnetic's Paper gameplay tests once the server has started, writes the results next to
 * the server jar and stops the server. Loaded only by the {@code runGameTest*} Gradle tasks.
 */
public final class MagneticGameTestPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new TestContext.TickEndDriver(), this);
        // The global region ticks once per server tick on Paper and Folia, so this runs after startup.
        Bukkit.getGlobalRegionScheduler().runDelayed(
            this,
            task -> new GameTestRunner(this, PaperGameTestScenarios.all()).start(),
            20L
        );
    }
}
