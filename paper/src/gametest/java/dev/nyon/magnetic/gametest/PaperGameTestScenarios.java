package dev.nyon.magnetic.gametest;

import dev.nyon.magnetic.common.conditions.ConditionChain;
import dev.nyon.magnetic.common.config.Config;
import dev.nyon.magnetic.common.config.Identifier;
import dev.nyon.magnetic.config.ConfigCacheKt;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.gametest.GameTestRunner.GameTest;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Fluid;
//? if >=26.1
import org.bukkit.GameRules;
//? if <26.1
//import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static dev.nyon.magnetic.gametest.TestContext.check;
import static dev.nyon.magnetic.gametest.TestContext.checkEquals;
import static dev.nyon.magnetic.gametest.TestContext.inventoryCount;

/**
 * Gameplay scenarios for the Paper plugin. They act through the same server code paths a real
 * player triggers (block breaking, entity damage, item use), so Magnetic's listeners are part of
 * every assertion. Keep tests that change global state, such as game rules, at the end.
 */
final class PaperGameTestScenarios {
    // The block that tests break or the mob they kill; players stand three blocks away so vanilla
    // pickup cannot reach normal drops before they are counted.
    private static final int PLAYER_DZ = -3;

    private PaperGameTestScenarios() {
    }

    static List<GameTest> all() {
        return List.of(
            new GameTest("default config ignores the ender dragon", PaperGameTestScenarios::defaultConfigIgnoresEnderDragon),
            new GameTest("drops stay on the ground without the enchantment", PaperGameTestScenarios::inactiveDropsNormally),
            new GameTest("block drops go straight into the inventory", PaperGameTestScenarios::blockDropsAreCollected),
            new GameTest("itemsAllowed=false leaves block drops on the ground", PaperGameTestScenarios::itemToggleKeepsDrops),
            new GameTest("block experience is collected", PaperGameTestScenarios::blockExperienceIsCollected),
            new GameTest("expAllowed=false leaves experience orbs", PaperGameTestScenarios::experienceToggleKeepsOrbs),
            new GameTest("ignored blocks drop normally", PaperGameTestScenarios::ignoredBlockDropsNormally),
            new GameTest("mob drops and experience are collected", PaperGameTestScenarios::mobDropsAreCollected),
            new GameTest("ignored mobs drop normally", PaperGameTestScenarios::ignoredMobDropsNormally),
            new GameTest("full inventory keeps the drop on the ground", PaperGameTestScenarios::fullInventoryKeepsDrop),
            new GameTest("SNEAK condition follows sneaking", PaperGameTestScenarios::sneakCondition),
            new GameTest("whole sugar cane column is collected", PaperGameTestScenarios::chainedBlocksAreCollected),
            new GameTest("sheared wool is collected", PaperGameTestScenarios::shearedWoolIsCollected),
            new GameTest("harvested berries are collected", PaperGameTestScenarios::harvestedBerriesAreCollected),
            new GameTest("animation pulls drops to their owner", PaperGameTestScenarios::animationPullsDrops),
            new GameTest("drops broken by bucket water are collected", PaperGameTestScenarios::bucketDropsAreCollected),
            new GameTest("drops in a dried-up bucket spot stay on the ground", PaperGameTestScenarios::driedUpBucketSpotKeepsDrops),
            new GameTest("full inventory keeps bucket drops in the world", PaperGameTestScenarios::fullInventoryKeepsBucketDrops),
            new GameTest("blocks that break with their support are collected", PaperGameTestScenarios::blocksBreakingWithTheirSupportAreCollected),
            new GameTest("/magnetic reload applies the config file", PaperGameTestScenarios::reloadCommandAppliesConfig),
            new GameTest("leaf decay drops are collected", PaperGameTestScenarios::leafDecayDropsAreCollected)
        );
    }

    private static void defaultConfigIgnoresEnderDragon(TestContext ctx) {
        check(
            ConfigCacheKt.getIgnoredEntities().contains(NamespacedKey.minecraft("ender_dragon")),
            "the default config does not ignore the ender dragon"
        );
        ctx.succeed();
    }

    private static void inactiveDropsNormally(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));
        breakBlock(ctx, player, Material.STONE);

        ctx.later(1, () -> {
            checkEquals(0, inventoryCount(player, Material.COBBLESTONE), "a plain pickaxe collected the drop");
            checkEquals(1, ctx.groundCount(Material.COBBLESTONE), "cobblestone on the ground");
            ctx.succeed();
        });
    }

    private static void blockDropsAreCollected(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));
        breakBlock(ctx, player, Material.STONE);

        checkEquals(1, inventoryCount(player, Material.COBBLESTONE), "cobblestone in the inventory right after breaking");
        checkEquals(1, player.getStatistic(Statistic.PICKUP, Material.COBBLESTONE), "cobblestone pickup statistic");
        ctx.later(2, () -> {
            checkEquals(0, ctx.groundCount(Material.COBBLESTONE), "cobblestone left on the ground");
            ctx.succeed();
        });
    }

    private static void itemToggleKeepsDrops(TestContext ctx) {
        Config config = testConfig("ENCHANTMENT");
        config.setItemsAllowed(false);
        apply(config);
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));
        breakBlock(ctx, player, Material.STONE);

        ctx.later(1, () -> {
            checkEquals(0, inventoryCount(player, Material.COBBLESTONE), "itemsAllowed=false still collected the drop");
            checkEquals(1, ctx.groundCount(Material.COBBLESTONE), "cobblestone on the ground");
            ctx.succeed();
        });
    }

    private static void blockExperienceIsCollected(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));
        breakBlock(ctx, player, Material.DIAMOND_ORE);

        checkEquals(1, inventoryCount(player, Material.DIAMOND), "diamond in the inventory");
        check(player.getTotalExperience() > 0, "diamond ore experience was not collected");
        ctx.later(2, () -> {
            checkEquals(0, ctx.orbCount(), "experience orbs left in the world");
            ctx.succeed();
        });
    }

    private static void experienceToggleKeepsOrbs(TestContext ctx) {
        Config config = testConfig("ENCHANTMENT");
        config.setExpAllowed(false);
        apply(config);
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));
        breakBlock(ctx, player, Material.DIAMOND_ORE);

        checkEquals(1, inventoryCount(player, Material.DIAMOND), "diamond in the inventory");
        checkEquals(0, player.getTotalExperience(), "expAllowed=false still collected experience");
        ctx.later(1, () -> {
            check(ctx.orbCount() > 0, "no experience orbs were dropped");
            ctx.succeed();
        });
    }

    private static void ignoredBlockDropsNormally(TestContext ctx) {
        Config config = testConfig("ENCHANTMENT");
        config.setIgnoreBlocks(List.of(new Identifier("minecraft", "diamond_ore", false)));
        apply(config);
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));
        breakBlock(ctx, player, Material.DIAMOND_ORE);

        ctx.later(1, () -> {
            checkEquals(0, inventoryCount(player, Material.DIAMOND), "an ignored block's drop was collected");
            checkEquals(0, player.getTotalExperience(), "an ignored block's experience was collected");
            checkEquals(1, ctx.groundCount(Material.DIAMOND), "diamond on the ground");
            check(ctx.orbCount() > 0, "no experience orbs were dropped");
            ctx.succeed();
        });
    }

    private static void mobDropsAreCollected(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_SWORD));
        killCow(ctx, player);

        check(inventoryCount(player, Material.BEEF) > 0, "beef was not collected");
        check(player.getTotalExperience() > 0, "cow experience was not collected");
        // Vanilla spawns death experience after the 20 tick death animation.
        ctx.later(25, () -> {
            checkEquals(0, ctx.groundCount(Material.BEEF), "beef left on the ground");
            checkEquals(0, ctx.orbCount(), "experience orbs left after the death animation");
            ctx.succeed();
        });
    }

    private static void ignoredMobDropsNormally(TestContext ctx) {
        Config config = testConfig("ENCHANTMENT");
        config.setIgnoreEntities(List.of(new Identifier("minecraft", "cow", false)));
        apply(config);
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_SWORD));
        killCow(ctx, player);

        checkEquals(0, inventoryCount(player, Material.BEEF), "an ignored mob's drop was collected");
        ctx.later(1, () -> check(ctx.groundCount(Material.BEEF) > 0, "no beef on the ground"));
        // Orbs appear after the death animation; check before vanilla attraction can deliver them.
        ctx.later(2, () -> ctx.await("the death animation drops experience orbs", 40, () -> ctx.orbCount() > 0, () -> {
            checkEquals(0, player.getTotalExperience(), "an ignored mob's experience was collected");
            ctx.succeed();
        }));
    }

    private static void fullInventoryKeepsDrop(TestContext ctx) {
        // Keep the default animation: with a full inventory Magnetic must fall back to dropping.
        Config config = testConfig("ENCHANTMENT");
        config.getAnimation().setEnabled(true);
        apply(config);
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));
        for (int slot = 1; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        breakBlock(ctx, player, Material.STONE);

        ctx.later(1, () -> {
            checkEquals(0, inventoryCount(player, Material.COBBLESTONE), "cobblestone fit into a full inventory");
            checkEquals(1, ctx.groundCount(Material.COBBLESTONE), "cobblestone on the ground");
            ctx.succeed();
        });
    }

    private static void sneakCondition(TestContext ctx) {
        apply(testConfig("SNEAK"));
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));
        breakBlock(ctx, player, Material.STONE);
        checkEquals(0, inventoryCount(player, Material.COBBLESTONE), "collected while not sneaking");

        ctx.later(1, () -> {
            checkEquals(1, ctx.groundCount(Material.COBBLESTONE), "cobblestone on the ground while not sneaking");
            player.setSneaking(true);
            breakBlock(ctx, player, Material.STONE);
            checkEquals(1, inventoryCount(player, Material.COBBLESTONE), "not collected while sneaking");
            ctx.succeed();
        });
    }

    private static void chainedBlocksAreCollected(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        ctx.block(0, -1, 0).setType(Material.SAND, false);
        ctx.block(1, -1, 0).setType(Material.WATER, false);
        for (int dy = 0; dy < 3; dy++) ctx.block(0, dy, 0).setType(Material.SUGAR_CANE, false);
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));
        check(player.breakBlock(ctx.block(0, 0, 0)), "the player could not break the sugar cane");

        // Vanilla breaks the upper sugar cane on the following ticks.
        ctx.later(10, () -> {
            check(ctx.block(0, 1, 0).getType().isAir() && ctx.block(0, 2, 0).getType().isAir(), "upper sugar cane was not removed");
            checkEquals(0, ctx.groundCount(Material.SUGAR_CANE), "sugar cane dropped on the ground");
            checkEquals(3, inventoryCount(player, Material.SUGAR_CANE), "sugar cane in the inventory after physics");
            ctx.succeed();
        });
    }

    private static void shearedWoolIsCollected(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        Player player = ctx.player(0, -1);
        player.getInventory().setItemInMainHand(magnetic(Material.SHEARS));
        Sheep sheep = (Sheep) ctx.world.spawnEntity(ctx.center(0, 0, 0), EntityType.SHEEP);
        sheep.setAI(false);
        sheep.setColor(DyeColor.WHITE);
        //? if >=26.1
        InteractionResult result = ((CraftEntity) sheep).getHandle().interact(nms(player), InteractionHand.MAIN_HAND, Vec3.ZERO);
        //? if <26.1
        //InteractionResult result = ((CraftEntity) sheep).getHandle().interact(nms(player), InteractionHand.MAIN_HAND);
        check(consumesAction(result), "the player could not shear the sheep: " + result);

        check(inventoryCount(player, Material.WHITE_WOOL) > 0, "wool was not collected");
        ctx.later(2, () -> {
            checkEquals(0, ctx.groundCount(Material.WHITE_WOOL), "wool left on the ground");
            ctx.succeed();
        });
    }

    private static void harvestedBerriesAreCollected(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        ctx.block(0, -1, 0).setType(Material.GRASS_BLOCK, false);
        Block bush = ctx.block(0, 0, 0);
        bush.setType(Material.SWEET_BERRY_BUSH, false);
        Ageable age = (Ageable) bush.getBlockData();
        age.setAge(age.getMaximumAge());
        bush.setBlockData(age, false);
        Player player = ctx.player(0, -1);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));

        BlockPos pos = new BlockPos(bush.getX(), bush.getY(), bush.getZ());
        ServerPlayer handle = nms(player);
        InteractionResult result = handle.gameMode.useItemOn(
            handle,
            ((CraftWorld) ctx.world).getHandle(),
            handle.getMainHandItem(),
            InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)
        );
        check(consumesAction(result), "the player could not harvest the bush: " + result);

        check(inventoryCount(player, Material.SWEET_BERRIES) > 0, "berries were not collected");
        ctx.later(2, () -> {
            checkEquals(0, ctx.groundCount(Material.SWEET_BERRIES), "berries left on the ground");
            ctx.succeed();
        });
    }

    private static void animationPullsDrops(TestContext ctx) {
        Config config = testConfig("ENCHANTMENT");
        config.getAnimation().setEnabled(true);
        apply(config);
        Player player = ctx.player(0, PLAYER_DZ);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_PICKAXE));
        breakBlock(ctx, player, Material.STONE);
        checkEquals(0, inventoryCount(player, Material.COBBLESTONE), "the animated drop skipped the animation");

        ctx.later(2, () -> {
            List<Item> pulled = ctx.groundItems(Material.COBBLESTONE);
            checkEquals(1, pulled.size(), "animated cobblestone entities");
            check(player.getUniqueId().equals(pulled.getFirst().getOwner()), "the animated drop is not owned by the player");
            ctx.await("the pulled cobblestone reaches the player", 200,
                () -> inventoryCount(player, Material.COBBLESTONE) == 1,
                () -> {
                    checkEquals(0, ctx.groundCount(Material.COBBLESTONE), "cobblestone left on the ground");
                    ctx.succeed();
                });
        });
    }

    private static void bucketDropsAreCollected(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        // Water from the bucket lands on the player's block and flows into the torch next to it.
        ctx.block(0, 0, 1).setType(Material.TORCH, false);
        Location standing = ctx.center(0, 0, 0);
        standing.setPitch(90f);
        Player player = ctx.player(standing);
        player.getInventory().setItemInMainHand(new ItemStack(Material.WATER_BUCKET));
        emptyBucket(ctx, player);

        ctx.await("the water breaks the torch", 100, () -> ctx.block(0, 0, 1).getType() != Material.TORCH, () ->
            ctx.later(5, () -> {
                checkEquals(0, ctx.groundCount(Material.TORCH), "torch left in the water");
                checkEquals(1, inventoryCount(player, Material.TORCH), "torch in the inventory");
                ctx.succeed();
            }));
    }

    /** Claimed drops that do not fit are put back into the world instead of being lost. */
    private static void fullInventoryKeepsBucketDrops(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        ctx.block(0, 0, 1).setType(Material.TORCH, false);
        Location standing = ctx.center(0, 0, 0);
        standing.setPitch(90f);
        Player player = ctx.player(standing);
        player.getInventory().setItemInMainHand(new ItemStack(Material.WATER_BUCKET));
        for (int slot = 1; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        emptyBucket(ctx, player);

        ctx.await("the water breaks the torch", 100, () -> ctx.block(0, 0, 1).getType() != Material.TORCH, () ->
            ctx.later(5, () -> {
                checkEquals(0, inventoryCount(player, Material.TORCH), "torch fit into a full inventory");
                checkEquals(1, ctx.groundCount(Material.TORCH), "torch back in the world");
                ctx.succeed();
            }));
    }

    private static void driedUpBucketSpotKeepsDrops(TestContext ctx) {
        apply(testConfig("ENCHANTMENT"));
        Location standing = ctx.center(0, 0, 0);
        standing.setPitch(90f);
        Player player = ctx.player(standing);
        player.getInventory().setItemInMainHand(new ItemStack(Material.WATER_BUCKET));
        emptyBucket(ctx, player);

        // Magnetic still tracks the spot for a while; a drop there without water is not the bucket's.
        ctx.later(2, () -> {
            ctx.block(0, 0, 0).setType(Material.AIR, false);
            ctx.world.dropItem(ctx.center(0, 0, 0), new ItemStack(Material.DIAMOND));
            ctx.later(5, () -> {
                checkEquals(0, inventoryCount(player, Material.DIAMOND), "a drop in a dried-up bucket spot was collected");
                List<Item> diamonds = ctx.groundItems(Material.DIAMOND);
                checkEquals(1, diamonds.size(), "diamonds on the ground");
                // The marker shows Magnetic claimed the drop and put it back, rather than never seeing it.
                check(
                    diamonds.getFirst().getPersistentDataContainer().has(new NamespacedKey("magnetic", "bucket_handled")),
                    "the drop was not claimed and restored by the bucket tracking"
                );
                ctx.succeed();
            });
        });
    }

    private static void emptyBucket(TestContext ctx, Player player) {
        ServerPlayer handle = nms(player);
        InteractionResult result = handle.gameMode.useItem(
            handle,
            ((CraftWorld) ctx.world).getHandle(),
            handle.getMainHandItem(),
            InteractionHand.MAIN_HAND
        );
        check(consumesAction(result), "the player could not empty the bucket: " + result);
        check(ctx.block(0, 0, 0).getType() == Material.WATER, "the bucket did not place water at the player");
    }

    private static void reloadCommandAppliesConfig(TestContext ctx) throws Exception {
        Path configFile = Path.of(Bukkit.getWorldContainer().getPath(), "plugins", "magnetic", "magnetic.json");
        String original = Files.readString(configFile);
        check(original.contains("\"itemsAllowed\": true"), "unexpected config file:\n" + original);
        ctx.onCleanupGlobal(() -> {
            try {
                Files.writeString(configFile, original);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Files.writeString(configFile, original.replace("\"itemsAllowed\": true", "\"itemsAllowed\": false"));

        ctx.global(
            () -> check(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "magnetic reload"), "/magnetic reload failed"),
            () -> {
                check(!ConfigKt.getConfig().getItemsAllowed(), "/magnetic reload did not apply itemsAllowed=false");
                ctx.succeed();
            }
        );
    }

    private static void leafDecayDropsAreCollected(TestContext ctx) {
        Config config = testConfig("ENCHANTMENT");
        config.getLeafDecay().setEnabled(true);
        apply(config);

        // A single log holding up a 5x5x5 canopy; enough leaves that some drop is all but certain.
        Block log = ctx.block(0, 2, 0);
        int leaves = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block block = ctx.block(dx, dy, dz);
                    if (block.equals(log)) continue;
                    Leaves data = (Leaves) Material.OAK_LEAVES.createBlockData();
                    data.setPersistent(false);
                    data.setDistance(Math.abs(dx) + Math.abs(dy - 2) + Math.abs(dz));
                    block.setBlockData(data, false);
                    leaves++;
                }
            }
        }
        log.setType(Material.OAK_LOG, false);
        int leafCount = leaves;
        Player player = ctx.player(0, -4);
        player.getInventory().setItemInMainHand(magnetic(Material.DIAMOND_AXE));
        Set<Material> leafDrops = Set.of(Material.OAK_SAPLING, Material.STICK, Material.APPLE);

        //? if >=26.1 {
        ctx.onCleanupGlobal(() -> ctx.world.setGameRule(GameRules.RANDOM_TICK_SPEED, 3));
        ctx.global(() -> ctx.world.setGameRule(GameRules.RANDOM_TICK_SPEED, 1000), () -> {
        //?} else {
        /*ctx.onCleanupGlobal(() -> ctx.world.setGameRule(GameRule.RANDOM_TICK_SPEED, 3));
        ctx.global(() -> ctx.world.setGameRule(GameRule.RANDOM_TICK_SPEED, 1000), () -> {
        *///?}
            check(player.breakBlock(log), "the player could not break the log");
            checkEquals(1, inventoryCount(player, Material.OAK_LOG), "log in the inventory");
            ctx.await("all " + leafCount + " leaves decay", 400, () -> countLeaves(ctx) == 0, () -> ctx.later(10, () -> {
                for (Material drop : leafDrops) {
                    checkEquals(0, ctx.groundCount(drop), drop + " left on the ground");
                }
                int collected = leafDrops.stream().mapToInt(drop -> inventoryCount(player, drop)).sum();
                check(collected > 0, "no leaf decay drops were collected");
                ctx.succeed();
            }));
        });
    }

    // Upward columns stand on the floor and hanging ones below a ceiling three blocks up. The grid
    // keeps a free block between columns, and the player stands outside it so vanilla pickup cannot
    // reach a drop.
    private static final int[][] COLUMN_GRID = columnGrid();
    private static final int COLUMN_HEIGHT = 3;

    private record Column(String name, List<BlockData> pieces, boolean hanging) {
    }

    /**
     * Blocks that cannot stay without the block below or above them, such as carpets or sugar cane,
     * break with it, in the same tick or a few ticks later. Magnetic collects them through Paper's
     * BlockDestroyEvent. The candidates come from the server's placement rules rather than a list,
     * so blocks added by a new Minecraft release are checked as well.
     */
    private static void blocksBreakingWithTheirSupportAreCollected(TestContext ctx) throws Exception {
        apply(testConfig("ENCHANTMENT"));
        Player player = ctx.player(0, 5);
        player.getInventory().setItemInMainHand(magnetic(Material.NETHERITE_PICKAXE));

        List<Material> blocks = new ArrayList<>();
        for (Material type : Material.values()) {
            if (type.isBlock() && !type.isAir() && !type.isLegacy()) blocks.add(type);
        }
        List<Column> columns = new ArrayList<>();
        for (Material type : blocks) {
            BlockData data = type.createBlockData();
            if (needsOwnKind(ctx, data, data, false)) columns.add(new Column(type.name(), List.of(data, data, data), false));
            if (needsOwnKind(ctx, data, data, true)) columns.add(new Column(type.name(), List.of(data, data, data), true));
        }
        Set<String> failures = new TreeSet<>();
        // Only columns that break over several ticks get heads; the others pop off at once.
        Set<String> popsAtOnce = new TreeSet<>();
        Set<Column> chains = new LinkedHashSet<>();
        runColumns(ctx, player, columns, 0, failures, popsAtOnce, chains, () -> {
            // Plant heads, such as a cactus flower, break once the column below them breaks.
            List<Column> heads = new ArrayList<>();
            for (Column chain : chains) {
                BlockData base = chain.pieces().getFirst();
                for (Material type : blocks) {
                    if (type.name().equals(chain.name()) || popsAtOnce.contains(type.name())) continue;
                    BlockData head = type.createBlockData();
                    if (!needsOwnKind(ctx, base, head, chain.hanging())) continue;
                    heads.add(new Column(chain.name() + "+" + type.name(), List.of(base, base, head), chain.hanging()));
                }
            }
            runColumns(ctx, player, heads, 0, failures, popsAtOnce, new LinkedHashSet<>(), () -> {
                check(failures.isEmpty(), "drops of blocks that broke with their support stayed on the ground: " + failures);
                ctx.succeed();
            });
        });
    }

    /**
     * Whether {@code piece} survives next to {@code support} (below it, or above it when hanging)
     * with another {@code piece} on its far side, but not with air in place of {@code support}.
     */
    private static boolean needsOwnKind(TestContext ctx, BlockData support, BlockData piece, boolean hanging) {
        Block near = ctx.block(4, hanging ? 3 : 1, 4);
        Block middle = ctx.block(4, 2, 4);
        Block far = ctx.block(4, hanging ? 1 : 3, 4);
        Block anchor = ctx.block(4, hanging ? 4 : 0, 4);
        anchor.setType(Material.STONE, false);
        far.setBlockData(piece, false);
        near.setBlockData(support, false);
        boolean supported = middle.canPlace(piece);
        near.setType(Material.AIR, false);
        boolean unsupported = middle.canPlace(piece);
        for (Block block : List.of(anchor, near, far)) block.setType(Material.AIR, false);
        return supported && !unsupported;
    }

    private static void runColumns(
        TestContext ctx, Player player, List<Column> columns, int start,
        Set<String> failures, Set<String> popsAtOnce, Set<Column> chains, TestContext.Step then
    ) throws Exception {
        if (start >= columns.size()) {
            then.run();
            return;
        }
        clearGrid(ctx);
        // A full inventory keeps drops on the ground, so only the tool stays between batches.
        ItemStack tool = player.getInventory().getItemInMainHand();
        player.getInventory().clear();
        player.getInventory().setItemInMainHand(tool);
        // Water plants turn into water when they break, which would flow into the other columns.
        boolean water = holdsWater(ctx, columns.get(start));
        int end = water ? start + 1 : start;
        if (!water) {
            while (end < columns.size() && end - start < COLUMN_GRID.length && !holdsWater(ctx, columns.get(end))) end++;
        }

        List<Runnable> checks = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Column column = columns.get(i);
            int[] cell = COLUMN_GRID[i - start];
            List<Block> pieces = build(ctx, cell, column);
            Block broken = pieces.getFirst();
            Block next = pieces.get(1);
            Material nextType = next.getType();
            if (!player.breakBlock(broken)) continue;
            boolean poppedAtOnce = next.getType() != nextType;
            if (poppedAtOnce) popsAtOnce.add(column.name());
            checks.add(() -> {
                if (next.getType() == nextType) return;
                if (!poppedAtOnce) chains.add(column);
                if (!ctx.world.getNearbyEntitiesByType(Item.class, ctx.center(cell[0], 1, cell[1]), 0.9, 3.0, 0.9).isEmpty()) {
                    failures.add(column.name() + (column.hanging() ? " (hanging)" : ""));
                }
            });
        }
        int resume = end;
        ctx.later(4, () -> {
            checks.forEach(Runnable::run);
            runColumns(ctx, player, columns, resume, failures, popsAtOnce, chains, then);
        });
    }

    /** Builds {@code column} at {@code cell} and returns its pieces, starting with the one the player breaks. */
    private static List<Block> build(TestContext ctx, int[] cell, Column column) {
        List<Block> pieces = new ArrayList<>();
        if (column.hanging()) ctx.block(cell[0], COLUMN_HEIGHT, cell[1]).setType(Material.STONE, false);
        for (int i = 0; i < COLUMN_HEIGHT; i++) {
            Block block = ctx.block(cell[0], column.hanging() ? COLUMN_HEIGHT - 1 - i : i, cell[1]);
            block.setBlockData(column.pieces().get(i), false);
            pieces.add(block);
        }
        return pieces;
    }

    private static boolean holdsWater(TestContext ctx, Column column) {
        Block probe = ctx.block(4, 1, 4);
        probe.setBlockData(column.pieces().getFirst(), false);
        boolean water = ctx.world.getFluidData(probe.getLocation()).getFluidType() != Fluid.EMPTY;
        probe.setType(Material.AIR, false);
        return water;
    }

    private static void clearGrid(TestContext ctx) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 3; dz++) {
                for (int dy = 0; dy <= COLUMN_HEIGHT; dy++) ctx.block(dx, dy, dz).setType(Material.AIR, false);
            }
        }
        for (Item item : ctx.world.getNearbyEntitiesByType(Item.class, ctx.center(0, 1, 0), 6.5, 4.0, 6.5)) item.remove();
    }

    private static int[][] columnGrid() {
        List<int[]> cells = new ArrayList<>();
        for (int dz = -4; dz <= 2; dz += 2) {
            for (int dx = -4; dx <= 4; dx += 2) cells.add(new int[]{dx, dz});
        }
        return cells.toArray(int[][]::new);
    }

    private static Config testConfig(String condition) {
        Config config = new Config();
        config.setConditionStatement(new ConditionChain(condition));
        config.getAnimation().setEnabled(false);
        config.getFullInventoryAlert().getSoundAlert().setEnabled(false);
        config.getFullInventoryAlert().getTextAlert().setEnabled(false);
        config.getFullInventoryAlert().getTitleAlert().setEnabled(false);
        return config;
    }

    private static void apply(Config config) {
        ConfigKt.setConfig(config);
    }

    private static ItemStack magnetic(Material type) {
        Enchantment enchantment = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.ENCHANTMENT)
            .getOrThrow(Key.key("magnetic", "magnetic"));
        ItemStack stack = new ItemStack(type);
        stack.addUnsafeEnchantment(enchantment, 1);
        return stack;
    }

    private static void breakBlock(TestContext ctx, Player player, Material type) {
        Block block = ctx.block(0, 0, 0);
        block.setType(type, false);
        check(player.breakBlock(block), "the player could not break " + type);
    }

    private static void killCow(TestContext ctx, Player player) {
        Cow cow = (Cow) ctx.world.spawnEntity(ctx.center(0, 0, 0), EntityType.COW);
        cow.setAI(false);
        cow.damage(1000.0, player);
        check(cow.isDead(), "the player could not kill the cow");
    }

    private static int countLeaves(TestContext ctx) {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (ctx.block(dx, dy, dz).getType() == Material.OAK_LEAVES) count++;
                }
            }
        }
        return count;
    }

    /**
     * InteractionResult was an enum on 1.21.1 and is an interface from 1.21.2, inside the 1.21.1
     * target's range. A direct call only links on one of them, so that target calls it reflectively.
     */
    private static boolean consumesAction(InteractionResult result) {
        //? if >=1.21.6 {
        return result.consumesAction();
        //?} else {
        /*try {
            return (boolean) result.getClass().getMethod("consumesAction").invoke(result);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read the interaction result " + result, e);
        }
        *///?}
    }

    private static ServerPlayer nms(Player player) {
        return ((CraftPlayer) player).getHandle();
    }
}
