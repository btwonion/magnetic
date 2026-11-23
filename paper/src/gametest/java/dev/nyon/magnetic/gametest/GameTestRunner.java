package dev.nyon.magnetic.gametest;

import dev.nyon.magnetic.config.Config;
import dev.nyon.magnetic.config.ConfigCacheKt;
import dev.nyon.magnetic.config.ConfigKt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the scenarios one after another on the region that owns the test site and stops the
 * server afterwards. Results go to {@value #RESULTS_FILE} in the server directory.
 */
final class GameTestRunner {
    static final String RESULTS_FILE = "magnetic-gametest-results.txt";
    private static final long RUN_TIMEOUT_MINUTES = 5;
    // Away from spawn, inside a single chunk so the whole site belongs to one Folia region.
    private static final int SITE_X = 40;
    private static final int SITE_Z = 40;

    @FunctionalInterface
    interface Body {
        void run(TestContext context) throws Exception;
    }

    record GameTest(String name, Body body) {
    }

    private final Plugin plugin;
    private final List<GameTest> tests;
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean finished = new AtomicBoolean();
    private int passed;
    private int failed;
    private Config originalConfig;
    private Location origin;

    GameTestRunner(Plugin plugin, List<GameTest> tests) {
        this.plugin = plugin;
        this.tests = tests;
    }

    void start() {
        World world = Bukkit.getWorlds().getFirst();
        Location site = new Location(world, SITE_X, 0, SITE_Z);
        originalConfig = ConfigKt.getConfig();

        Bukkit.getAsyncScheduler().runDelayed(
            plugin,
            task -> finish("the run did not finish within " + RUN_TIMEOUT_MINUTES + " minutes"),
            RUN_TIMEOUT_MINUTES,
            TimeUnit.MINUTES
        );
        world.getChunkAtAsync(site).thenAccept(chunk -> Bukkit.getRegionScheduler().execute(plugin, site, () -> {
            chunk.addPluginChunkTicket(plugin);
            origin = new Location(world, SITE_X, world.getHighestBlockYAt(SITE_X, SITE_Z) + 1, SITE_Z);
            plugin.getLogger().info("Running " + tests.size() + " Magnetic gameplay tests at " + origin.toVector()
                + " on " + (TestContext.FOLIA ? "Folia" : "Paper") + " " + Bukkit.getMinecraftVersion());
            runTest(0);
        }));
    }

    private void runTest(int index) {
        if (finished.get()) return;
        if (index >= tests.size()) {
            finish(null);
            return;
        }

        GameTest test = tests.get(index);
        TestContext context = new TestContext(plugin, origin, test.name(), error -> {
            if (error == null) {
                passed++;
                lines.add("PASS " + test.name());
                plugin.getLogger().info("PASS " + test.name());
            } else {
                failed++;
                lines.add("FAIL " + test.name() + ": " + describe(error));
                plugin.getLogger().severe("FAIL " + test.name() + ": " + describe(error));
                if (!(error instanceof AssertionError)) {
                    StringWriter trace = new StringWriter();
                    error.printStackTrace(new PrintWriter(trace));
                    lines.add(trace.toString().strip());
                    plugin.getLogger().severe(trace.toString());
                }
            }
            resetConfig();
            // Give removed players and entities a tick to leave the world before the next test.
            Bukkit.getRegionScheduler().runDelayed(plugin, origin, task -> runTest(index + 1), 2L);
        });
        context.begin(test.body());
    }

    private void resetConfig() {
        ConfigKt.setConfig(originalConfig);
        ConfigCacheKt.reloadIgnoredBlocks();
        ConfigCacheKt.reloadIgnoredEntities();
    }

    private void finish(String abortReason) {
        if (!finished.compareAndSet(false, true)) return;

        List<String> report = new ArrayList<>();
        if (abortReason != null) {
            failed++;
            report.add("ABORTED: " + abortReason);
        }
        synchronized (lines) {
            report.addAll(lines);
        }
        String status = failed == 0 && passed == tests.size() ? "PASSED" : "FAILED";
        report.add(status + ": " + passed + " passed, " + failed + " failed, " + tests.size() + " total");
        try {
            Files.write(Path.of(Bukkit.getWorldContainer().getPath(), RESULTS_FILE), report);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not write the test results: " + e);
        }
        plugin.getLogger().info(report.getLast());
        Bukkit.getGlobalRegionScheduler().execute(plugin, Bukkit::shutdown);
    }

    private static String describe(Throwable error) {
        return error.getMessage() == null ? error.getClass().getName() : error.getMessage();
    }
}
