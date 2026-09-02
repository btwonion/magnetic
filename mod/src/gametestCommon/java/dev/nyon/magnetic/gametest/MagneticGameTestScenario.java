package dev.nyon.magnetic.gametest;

import dev.nyon.magnetic.Animation;
import dev.nyon.magnetic.config.Config;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.config.conditions.ConditionChain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Shared loader-independent gameplay assertions. Loader adapters only register this scenario. */
public final class MagneticGameTestScenario {
    private static final BlockPos DROP_POS = new BlockPos(2, 1, 2);

    private MagneticGameTestScenario() {
    }

    public static void run(GameTestHelper helper) {
        Config previousConfig = ConfigKt.getConfig();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 1))));

        try {
            verifyInactiveDropsNormally(helper, player);
            verifyItemsAreCollected(helper, player);
            verifyItemTogglePreservesDrop(helper, player);
            verifyExperienceIsCollected(helper, player);
            verifyFullInventoryPreservesDrop(helper, player);
            verifyFireKillUsesPlayerKillCredit(helper, player);
            verifyRemovedAnimatedItemsAreUntracked(helper, player);
            helper.succeed();
        } finally {
            ConfigKt.setConfig(previousConfig);
        }
    }

    private static void verifyInactiveDropsNormally(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("SNEAK", true, true));
        breakBlock(helper, player, Blocks.DIRT);

        assertEquals(helper, 0, count(player, Items.DIRT), "inactive Magnetic changed the inventory");
        helper.assertItemEntityPresent(Items.DIRT, DROP_POS, 2.0);
        helper.despawnItem(DROP_POS, 2.0);
    }

    private static void verifyRemovedAnimatedItemsAreUntracked(GameTestHelper helper, ServerPlayer player) {
        Config config = testConfig("", true, true);
        config.getAnimation().setEnabled(true);
        ConfigKt.setConfig(config);

        ItemEntity itemEntity = Animation.INSTANCE.pullItemToPlayer(
            new ItemStack(Items.DIRT),
            Vec3.atCenterOf(helper.absolutePos(DROP_POS)),
            player
        );
        if (!Animation.INSTANCE.tracksItem(itemEntity)) {
            fail(helper, "animation did not track its spawned item");
        }

        itemEntity.discard();
        Animation.INSTANCE.tick();

        if (Animation.INSTANCE.tracksItem(itemEntity)) {
            fail(helper, "animation retained a removed item");
        }
    }

    private static void verifyItemsAreCollected(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("", true, true));
        breakBlock(helper, player, Blocks.DIRT);

        assertEquals(helper, 1, count(player, Items.DIRT), "active Magnetic did not collect the block drop");
        helper.assertItemEntityNotPresent(Items.DIRT, DROP_POS, 2.0);
        player.getInventory().clearContent();
    }

    private static void verifyItemTogglePreservesDrop(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("", false, true));
        breakBlock(helper, player, Blocks.DIRT);

        assertEquals(helper, 0, count(player, Items.DIRT), "itemsAllowed=false still changed the inventory");
        helper.assertItemEntityPresent(Items.DIRT, DROP_POS, 2.0);
        helper.despawnItem(DROP_POS, 2.0);
    }

    private static void verifyExperienceIsCollected(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("", true, true));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        breakBlock(helper, player, Blocks.DIAMOND_ORE);

        if (count(player, Items.DIAMOND) == 0) {
            fail(helper, "active Magnetic did not collect the ore drop");
        }
        if (player.totalExperience == 0) {
            fail(helper, "active Magnetic did not collect experience");
        }
        helper.assertEntityNotPresent(EntityTypes.EXPERIENCE_ORB);
        player.getInventory().clearContent();
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0;
    }

    private static void verifyFullInventoryPreservesDrop(GameTestHelper helper, ServerPlayer player) {
        Config config = testConfig("", true, true);
        config.getAnimation().setEnabled(true);
        ConfigKt.setConfig(config);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        breakBlock(helper, player, Blocks.DIRT);

        helper.assertItemEntityPresent(Items.DIRT, DROP_POS, 2.0);
        player.getInventory().clearContent();
        helper.despawnItem(DROP_POS, 2.0);
    }

    private static void verifyFireKillUsesPlayerKillCredit(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("", true, true));

        LivingEntity uncreditedCow = helper.spawn(EntityTypes.COW, DROP_POS);
        uncreditedCow.igniteForSeconds(5.0F);
        if (!uncreditedCow.hurtServer(helper.getLevel(), uncreditedCow.damageSources().onFire(), Float.MAX_VALUE)) {
            fail(helper, "uncredited fire damage did not kill the control mob");
        }
        assertEquals(helper, 0, count(player, Items.COOKED_BEEF), "uncredited fire kill changed the inventory");
        helper.assertItemEntityPresent(Items.COOKED_BEEF, DROP_POS, 2.0);
        helper.despawnItem(DROP_POS, 2.0);

        LivingEntity creditedCow = helper.spawn(EntityTypes.COW, DROP_POS);
        Arrow arrow = new Arrow(
            helper.getLevel(),
            player,
            new ItemStack(Items.ARROW),
            new ItemStack(Items.BOW)
        );
        if (!creditedCow.hurtServer(helper.getLevel(), creditedCow.damageSources().arrow(arrow, player), 1.0F)) {
            fail(helper, "player-owned arrow did not damage the regression-test mob");
        }
        creditedCow.igniteForSeconds(5.0F);
        if (!creditedCow.hurtServer(helper.getLevel(), creditedCow.damageSources().onFire(), Float.MAX_VALUE)) {
            fail(helper, "credited fire damage did not kill the regression-test mob");
        }

        if (count(player, Items.COOKED_BEEF) == 0) {
            fail(helper, "fire kill credited to a player did not collect the mob drop");
        }
        helper.assertItemEntityNotPresent(Items.COOKED_BEEF, DROP_POS, 2.0);
        player.getInventory().clearContent();
    }

    private static Config testConfig(String condition, boolean itemsAllowed, boolean expAllowed) {
        Config config = new Config();
        config.setConditionStatement(new ConditionChain(condition));
        config.setItemsAllowed(itemsAllowed);
        config.setExpAllowed(expAllowed);
        config.getAnimation().setEnabled(false);
        config.getFullInventoryAlert().getSoundAlert().setEnabled(false);
        config.getFullInventoryAlert().getTextAlert().setEnabled(false);
        config.getFullInventoryAlert().getTitleAlert().setEnabled(false);
        return config;
    }

    private static void breakBlock(GameTestHelper helper, ServerPlayer player, net.minecraft.world.level.block.Block block) {
        helper.setBlock(DROP_POS, block);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 1))));
        if (!player.gameMode.destroyBlock(helper.absolutePos(DROP_POS))) {
            fail(helper, "the test player could not break " + block);
        }
    }

    private static int count(ServerPlayer player, Item item) {
        int result = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) result += stack.getCount();
        }
        return result;
    }

    private static void assertEquals(GameTestHelper helper, int expected, int actual, String message) {
        if (expected != actual) fail(helper, message + ": expected " + expected + ", got " + actual);
    }

    private static void fail(GameTestHelper helper, String message) {
        throw helper.assertionException(Component.literal(message));
    }
}
