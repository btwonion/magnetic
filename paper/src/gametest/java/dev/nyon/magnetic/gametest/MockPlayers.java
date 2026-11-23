package dev.nyon.magnetic.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
//? if <26.1 {
/*import net.minecraft.server.players.PlayerList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.lang.reflect.Method;
import java.util.Optional;
*///?}

/**
 * Joins server-side players without a client, the way vanilla's {@code GameTestHelper} does.
 * They go through Paper's normal join path, so plugins see a regular online {@link Player}.
 */
final class MockPlayers {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private MockPlayers() {
    }

    /** Must run on the thread that owns {@code location}; Folia registers the connection with that region. */
    static Player join(Location location) {
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "magnetic_test" + COUNTER.incrementAndGet());
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        // Folia resets players to their last accepted position every tick, so place them before joining.
        // Entity#moveTo was renamed to snapTo inside the 1.21.1 target's range; these calls exist on every release.
        player.setPos(location.getX(), location.getY(), location.getZ());
        player.setYRot(location.getYaw());
        player.setXRot(location.getPitch());
        player.setOldPosAndRot();

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // Folia's disconnect handling expects an IP address rather than Netty's embedded one.
        connection.address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        //? if >=26.1
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        //? if <26.1
        //placeAtTestSite(server.getPlayerList(), connection, player, cookie, location);
        return player.getBukkitEntity();
    }

    // Paper 1.21.1 to at least 1.21.6 moves first-time players to the world spawn while they join,
    // and Folia before 1.21.6 joins them through a method that takes the spawn location.
    //? if <26.1 {
    /*private static Location pendingSpawn;

    private static void placeAtTestSite(
        PlayerList players, Connection connection, ServerPlayer player, CommonListenerCookie cookie, Location location
    ) {
        Method foliaJoin;
        try {
            foliaJoin = PlayerList.class.getMethod("placeNewPlayer", Connection.class, ServerPlayer.class,
                CommonListenerCookie.class, Optional.class, String.class, Location.class);
        } catch (NoSuchMethodException paper) {
            foliaJoin = null;
        }
        if (foliaJoin != null) {
            try {
                foliaJoin.invoke(players, connection, player, cookie, Optional.empty(), player.getGameProfile().getName(), location);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot join a test player on Folia", e);
            }
            return;
        }

        pendingSpawn = location;
        try {
            players.placeNewPlayer(connection, player, cookie);
        } finally {
            pendingSpawn = null;
        }
    }

    /^* Keeps players that {@link #join} places at their test site instead of the world spawn. ^/
    static final class SpawnAtTestSite implements Listener {
        @EventHandler
        public void onSpawnLocation(PlayerSpawnLocationEvent event) {
            if (pendingSpawn != null) event.setSpawnLocation(pendingSpawn);
        }
    }
    *///?}

    /**
     * Ticks the player like a connected client would. Folia ticks the connections of each region,
     * Paper only ticks connections owned by its network listener, so tests tick them manually there.
     */
    static void tick(Player player) {
        ((CraftPlayer) player).getHandle().connection.tick();
    }
}
