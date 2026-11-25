package dev.nyon.magnetic.gametest;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * State and scheduling helpers for one test.
 *
 * <p>Steps run at the end of a tick on the thread that owns the test site. That is where real
 * players act: Paper handles packets between ticks and Folia in each region's connection tick,
 * both after the world ticked. Breaking blocks earlier in the tick would let vanilla block ticks
 * run before Magnetic's next-tick work, which players cannot do. A thrown exception or failed
 * assertion ends the test.
 */
final class TestContext {
    static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    private static final int SITE_RADIUS = 5;
    private static final int SITE_HEIGHT = 8;
    private static final int TEST_TIMEOUT_TICKS = 600;
    private static volatile TestContext active;

    @FunctionalInterface
    interface Step {
        void run() throws Exception;
    }

    private record Pending(long dueTick, Step step) {
    }

    /** Drives the active test from the end of every tick of the region that owns its site. */
    static final class TickEndDriver implements Listener {
        @EventHandler
        public void onTickEnd(ServerTickEndEvent event) {
            TestContext context = active;
            if (context != null) context.onTickEnd();
        }
    }

    final Plugin plugin;
    final Location origin;
    final World world;
    final String name;
    private final Consumer<Throwable> onComplete;
    private final List<Player> players = new ArrayList<>();
    private final List<Runnable> globalCleanups = new ArrayList<>();
    private final Queue<Pending> pending = new ConcurrentLinkedQueue<>();
    private final AtomicLong tick = new AtomicLong();
    private volatile boolean done;

    TestContext(Plugin plugin, Location origin, String name, Consumer<Throwable> onComplete) {
        this.plugin = plugin;
        this.origin = origin;
        this.world = origin.getWorld();
        this.name = name;
        this.onComplete = onComplete;
    }

    void begin(GameTestRunner.Body body) {
        active = this;
        later(1, () -> {
            resetSite();
            body.run(this);
        });
    }

    // Positions are relative to the origin, the air block above the floor in the middle of the site.

    Block block(int dx, int dy, int dz) {
        return world.getBlockAt(origin.getBlockX() + dx, origin.getBlockY() + dy, origin.getBlockZ() + dz);
    }

    Location center(int dx, int dy, int dz) {
        return block(dx, dy, dz).getLocation().add(0.5, 0.0, 0.5);
    }

    /** Joins a client-less player standing at the given block, looking along +Z unless rotated. */
    Player player(int dx, int dz) {
        return player(center(dx, 0, dz));
    }

    Player player(Location location) {
        Player player = MockPlayers.join(location);
        players.add(player);
        return player;
    }

    /** Continues with {@code step} at the end of a later tick; safe to call from any thread. */
    void later(int ticks, Step step) {
        pending.add(new Pending(tick.get() + Math.max(1, ticks), step));
    }

    /** Polls {@code condition} once per tick and continues with {@code then} once it holds. */
    void await(String what, int timeoutTicks, BooleanSupplier condition, Step then) {
        if (condition.getAsBoolean()) {
            guard(then);
        } else if (timeoutTicks <= 0) {
            throw new AssertionError("timed out waiting until " + what);
        } else {
            later(1, () -> await(what, timeoutTicks - 1, condition, then));
        }
    }

    /** Runs {@code step} on the global region, which owns game rules on Folia, then continues here. */
    void global(Runnable step, Step then) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            try {
                step.run();
                later(1, then);
            } catch (Throwable error) {
                later(1, () -> {
                    throw error;
                });
            }
        });
    }

    /** Registers global-region work that undoes a change once the test ends, even when it fails. */
    void onCleanupGlobal(Runnable cleanup) {
        globalCleanups.add(cleanup);
    }

    void succeed() {
        complete(null);
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void checkEquals(int expected, int actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    List<Item> groundItems(Material type) {
        return nearby(Item.class).stream().filter(item -> item.getItemStack().getType() == type).toList();
    }

    int groundCount(Material type) {
        return groundItems(type).stream().mapToInt(item -> item.getItemStack().getAmount()).sum();
    }

    int orbCount() {
        return nearby(ExperienceOrb.class).size();
    }

    static int inventoryCount(Player player, Material type) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == type) count += stack.getAmount();
        }
        return count;
    }

    private <T extends Entity> List<T> nearby(Class<T> type) {
        double radius = SITE_RADIUS + 1.5;
        return new ArrayList<>(world.getNearbyEntitiesByType(
            type,
            origin.clone().add(0.5, SITE_HEIGHT / 2.0, 0.5),
            radius,
            SITE_HEIGHT / 2.0 + 1.5,
            radius
        ));
    }

    private void onTickEnd() {
        if (done || FOLIA && !Bukkit.isOwnedByCurrentRegion(origin)) return;
        long now = tick.incrementAndGet();
        if (now > TEST_TIMEOUT_TICKS) {
            complete(new AssertionError("did not finish within " + TEST_TIMEOUT_TICKS + " ticks"));
            return;
        }
        tickPlayers();

        List<Pending> due = new ArrayList<>();
        pending.removeIf(entry -> entry.dueTick() <= now && due.add(entry));
        for (Pending entry : due) guard(entry.step());
    }

    private void guard(Step step) {
        if (done) return;
        try {
            step.run();
        } catch (Throwable error) {
            complete(error);
        }
    }

    /** Folia ticks every region's connections; Paper only ticks real network connections. */
    private void tickPlayers() {
        if (FOLIA) return;
        for (Player player : players) {
            if (player.isOnline()) MockPlayers.tick(player);
        }
    }

    private void complete(Throwable error) {
        if (done) return;
        done = true;
        active = null;
        pending.clear();

        Throwable result = error;
        try {
            for (Player player : players) {
                if (player.isOnline()) player.kick(Component.text("Magnetic gameplay test finished"));
            }
            resetSite();
        } catch (Throwable cleanupError) {
            if (result == null) result = cleanupError;
        }

        Throwable finalResult = result;
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            globalCleanups.forEach(Runnable::run);
            Bukkit.getRegionScheduler().execute(plugin, origin, () -> onComplete.accept(finalResult));
        });
    }

    /** Clears the site to air above a stone floor and removes every entity except players. */
    private void resetSite() {
        for (int dx = -SITE_RADIUS; dx <= SITE_RADIUS; dx++) {
            for (int dz = -SITE_RADIUS; dz <= SITE_RADIUS; dz++) {
                block(dx, -2, dz).setType(Material.STONE, false);
                block(dx, -1, dz).setType(Material.STONE, false);
                for (int dy = 0; dy <= SITE_HEIGHT; dy++) block(dx, dy, dz).setType(Material.AIR, false);
            }
        }
        for (Entity entity : nearby(Entity.class)) {
            if (!(entity instanceof Player)) entity.remove();
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
