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
        player.snapTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // Folia's disconnect handling expects an IP address rather than Netty's embedded one.
        connection.address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player.getBukkitEntity();
    }

    /**
     * Ticks the player like a connected client would. Folia ticks the connections of each region,
     * Paper only ticks connections owned by its network listener, so tests tick them manually there.
     */
    static void tick(Player player) {
        ((CraftPlayer) player).getHandle().connection.tick();
    }
}
