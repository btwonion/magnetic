package dev.nyon.magnetic.compat.treeharvester;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class TreeHarvesterLeafTracker {

    private static final long TIMEOUT_MILLIS = 300_000L;
    private static final Map<ServerLevel, Map<BlockPos, Entry>> ENTRIES = new WeakHashMap<>();

    private TreeHarvesterLeafTracker() {
    }

    public static synchronized void record(
        ServerLevel level,
        BlockPos pos,
        ServerPlayer player
    ) {
        long now = System.currentTimeMillis();
        Map<BlockPos, Entry> levelEntries = ENTRIES.computeIfAbsent(level, ignored -> new HashMap<>());
        levelEntries.values().removeIf(entry -> now - entry.timestamp() > TIMEOUT_MILLIS);
        levelEntries.put(pos.immutable(), new Entry(player, now));
    }

    @Nullable
    public static synchronized ServerPlayer lookup(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Entry> levelEntries = ENTRIES.get(level);
        if (levelEntries == null) return null;

        long now = System.currentTimeMillis();
        levelEntries.values().removeIf(entry -> now - entry.timestamp() > TIMEOUT_MILLIS);
        Entry entry = levelEntries.get(pos);
        if (levelEntries.isEmpty()) ENTRIES.remove(level);
        return entry == null ? null : entry.player();
    }

    @Nullable
    public static synchronized ServerPlayer take(ServerLevel level, BlockPos pos) {
        ServerPlayer player = lookup(level, pos);
        Map<BlockPos, Entry> levelEntries = ENTRIES.get(level);
        if (levelEntries == null) return player;

        levelEntries.remove(pos);
        if (levelEntries.isEmpty()) ENTRIES.remove(level);
        return player;
    }

    public static synchronized void cleanup(ServerLevel level) {
        Map<BlockPos, Entry> levelEntries = ENTRIES.get(level);
        if (levelEntries == null) return;

        long now = System.currentTimeMillis();
        levelEntries.values().removeIf(entry -> now - entry.timestamp() > TIMEOUT_MILLIS);
        if (levelEntries.isEmpty()) ENTRIES.remove(level);
    }

    private record Entry(ServerPlayer player, long timestamp) {
    }
}
