package dev.nyon.magnetic.gametest;

import dev.nyon.magnetic.config.Config;
import dev.nyon.magnetic.config.ConfigCacheKt;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.config.Identifier;
import dev.nyon.magnetic.config.conditions.ConditionChain;
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
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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
import java.util.List;
import java.util.Set;

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
        config.setIgnoreBlocks(List.of(new Identifier(NamespacedKey.minecraft("diamond_ore"), false)));
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
        config.setIgnoreEntities(List.of(new Identifier(NamespacedKey.minecraft("cow"), false)));
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

        checkEquals(3, inventoryCount(player, Material.SUGAR_CANE), "sugar cane in the inventory");
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
        InteractionResult result = ((CraftEntity) sheep).getHandle().interact(nms(player), InteractionHand.MAIN_HAND, Vec3.ZERO);
        check(result.consumesAction(), "the player could not shear the sheep: " + result);

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
        check(result.consumesAction(), "the player could not harvest the bush: " + result);

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
        check(result.consumesAction(), "the player could not empty the bucket: " + result);
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

        ctx.onCleanupGlobal(() -> ctx.world.setGameRule(GameRules.RANDOM_TICK_SPEED, 3));
        ctx.global(() -> ctx.world.setGameRule(GameRules.RANDOM_TICK_SPEED, 1000), () -> {
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

    // Helpers

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
        ConfigCacheKt.reloadIgnoredBlocks();
        ConfigCacheKt.reloadIgnoredEntities();
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

    private static ServerPlayer nms(Player player) {
        return ((CraftPlayer) player).getHandle();
    }
}
