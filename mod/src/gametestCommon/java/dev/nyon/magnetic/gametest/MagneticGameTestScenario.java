package dev.nyon.magnetic.gametest;

import dev.nyon.magnetic.Animation;
import dev.nyon.magnetic.DropEvent;
import dev.nyon.magnetic.compat.SmeltingDatapackCompat;
import dev.nyon.magnetic.config.Config;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.config.conditions.ConditionChain;
import dev.nyon.magnetic.extensions.MagneticCheckKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;

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
            verifyIgnoredBlockDropsNormally(helper, player);
            verifyEntityExperienceIsCollected(helper, player);
            verifyIgnoredEntityDropsNormally(helper, player);
            verifyEnderDragonIsIgnoredByDefault(helper);
            verifyFullInventoryPreservesDrop(helper, player);
            verifyPickupStatistics(helper, player);
            verifyFireKillUsesPlayerKillCredit(helper, player);
            verifyRemovedAnimatedItemsAreUntracked(helper, player);
            verifyBucketPlacementIsTracked(helper, player);
            verifyAutoSmeltDatapackDropsAreDeferred(helper, player);
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
        resetExperience(player);
    }

    private static void verifyIgnoredBlockDropsNormally(GameTestHelper helper, ServerPlayer player) {
        Config config = testConfig("", true, true);
        config.setIgnoreBlocks(List.of(ignoreEntry(BuiltInRegistries.BLOCK.getKey(Blocks.DIAMOND_ORE))));
        ConfigKt.setConfig(config);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        breakBlock(helper, player, Blocks.DIAMOND_ORE);

        assertEquals(helper, 0, count(player, Items.DIAMOND), "ignored block drop changed the inventory");
        assertEquals(helper, 0, player.totalExperience, "ignored block experience was collected");
        helper.assertItemEntityPresent(Items.DIAMOND, DROP_POS, 2.0);
        helper.assertEntityPresent(EntityTypes.EXPERIENCE_ORB, DROP_POS, 2.0);
        clearDrops(helper, player);
    }

    private static void verifyEntityExperienceIsCollected(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("", true, true));
        killWithPlayerCredit(helper, player, EntityTypes.COW);

        if (count(player, Items.BEEF) == 0) {
            fail(helper, "active Magnetic did not collect the mob drop");
        }
        if (player.totalExperience == 0) {
            fail(helper, "active Magnetic did not collect mob experience");
        }
        helper.assertEntityNotPresent(EntityTypes.EXPERIENCE_ORB);
        clearDrops(helper, player);
    }

    private static void verifyIgnoredEntityDropsNormally(GameTestHelper helper, ServerPlayer player) {
        Config config = testConfig("", true, true);
        config.setIgnoreEntities(List.of(ignoreEntry(EntityType.getKey(EntityTypes.COW))));
        ConfigKt.setConfig(config);
        killWithPlayerCredit(helper, player, EntityTypes.COW);

        assertEquals(helper, 0, count(player, Items.BEEF), "ignored mob drop changed the inventory");
        assertEquals(helper, 0, player.totalExperience, "ignored mob experience was collected");
        helper.assertItemEntityPresent(Items.BEEF, DROP_POS, 2.0);
        helper.assertEntityPresent(EntityTypes.EXPERIENCE_ORB, DROP_POS, 2.0);
        clearDrops(helper, player);

        // 1.21.x delivers melee kills inside the interaction packet scope, which already names the player.
        ThreadLocalScope.run(
            MixinHelper.threadLocal,
            player,
            () -> killWithPlayerCredit(helper, player, EntityTypes.COW)
        );

        assertEquals(helper, 0, count(player, Items.BEEF), "ignored mob drop inside a player scope changed the inventory");
        assertEquals(helper, 0, player.totalExperience, "ignored mob experience inside a player scope was collected");
        helper.assertItemEntityPresent(Items.BEEF, DROP_POS, 2.0);
        helper.assertEntityPresent(EntityTypes.EXPERIENCE_ORB, DROP_POS, 2.0);
        clearDrops(helper, player);
    }

    private static void verifyEnderDragonIsIgnoredByDefault(GameTestHelper helper) {
        ConfigKt.setConfig(new Config());
        if (!MagneticCheckKt.isIgnored(EntityTypes.ENDER_DRAGON)) {
            fail(helper, "the default config does not ignore the ender dragon");
        }
        if (MagneticCheckKt.isIgnored(EntityTypes.COW)) {
            fail(helper, "the default config ignores an unlisted entity");
        }
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

    private static void verifyPickupStatistics(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("", true, true));
        int before = pickupStat(player, Items.DIRT);
        breakBlock(helper, player, Blocks.DIRT);
        assertEquals(helper, before + 1, pickupStat(player, Items.DIRT), "collected block drop was not counted as picked up");
        player.getInventory().clearContent();

        // Only 4 of 10 items fit; those 4 must still be counted while the remaining 6 stay in the drop list.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 60));
        before = pickupStat(player, Items.DIRT);
        List<ItemStack> drops = new ArrayList<>(List.of(new ItemStack(Items.DIRT, 10)));
        DropEvent.INSTANCE.invoke(drops, new MutableInt(0), player, helper.absolutePos(DROP_POS));

        assertEquals(helper, before + 4, pickupStat(player, Items.DIRT), "partial pickup was not counted");
        assertEquals(helper, 1, drops.size(), "partial pickup removed the leftover drop");
        assertEquals(helper, 6, drops.get(0).getCount(), "partial pickup left the wrong drop amount");
        player.getInventory().clearContent();
    }

    private static int pickupStat(ServerPlayer player, Item item) {
        return player.getStats().getValue(Stats.ITEM_PICKED_UP.get(item));
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

    private static void verifyBucketPlacementIsTracked(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("", true, true));
        verifyBucketPlacement(helper, player, (BucketItem) Items.WATER_BUCKET, new BlockPos(4, 1, 4));
        // MobBucketItem overrides the placement sound without calling super, so it needs its own hook.
        verifyBucketPlacement(helper, player, (BucketItem) Items.COD_BUCKET, new BlockPos(0, 1, 4));
    }

    private static void verifyBucketPlacement(GameTestHelper helper, ServerPlayer player, BucketItem bucket, BlockPos relativePos) {
        ServerLevel level = helper.getLevel();
        PositionTracker tracker = ((ServerLevelHolder) level).getPositionTracker();
        BlockPos pos = helper.absolutePos(relativePos);
        helper.setBlock(relativePos, Blocks.AIR);
        try {
            if (!bucket.emptyContents(player, level, pos, null)) {
                fail(helper, "the test player could not empty " + bucket);
            }
            if (tracker.lookup(pos) != player) {
                fail(helper, "placing fluid with " + bucket + " was not attributed to the player");
            }
        } finally {
            helper.setBlock(relativePos, Blocks.AIR);
        }
    }

    /**
     * Auto-smelt datapacks such as Smelt Mining smelt item entities after the block broke. The test mod ships a
     * stand-in for the Smelt Mining enchantment; with it, drops must spawn and only be collected after the delay.
     */
    private static void verifyAutoSmeltDatapackDropsAreDeferred(GameTestHelper helper, ServerPlayer player) {
        ConfigKt.setConfig(testConfig("", true, true));
        clearDrops(helper, player);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(
            helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                ResourceKey.create(
                    Registries.ENCHANTMENT,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("smelt_mining", "smelt_mining")
                )
            ),
            1
        );

        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
            breakBlock(helper, player, Blocks.ANCIENT_DEBRIS);
            assertEquals(helper, 0, count(player, Items.ANCIENT_DEBRIS), "drop was collected before the datapack could smelt it");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(helper.absolutePos(DROP_POS)).inflate(2.0),
                entity -> entity.getItem().is(Items.ANCIENT_DEBRIS)
            );
            assertEquals(helper, 1, drops.size(), "auto-smelt datapack tool did not let the drop spawn");

            // Stand in for the datapack's smelting function.
            drops.getFirst().setItem(new ItemStack(Items.NETHERITE_SCRAP));
            SmeltingDatapackCompat.collectDue(helper.getLevel(), helper.getLevel().getGameTime() - 1);
            helper.assertItemEntityPresent(Items.NETHERITE_SCRAP, DROP_POS, 2.0);

            SmeltingDatapackCompat.collectDue(
                helper.getLevel(),
                helper.getLevel().getGameTime() + SmeltingDatapackCompat.COLLECTION_DELAY_TICKS
            );
            assertEquals(helper, 1, count(player, Items.NETHERITE_SCRAP), "smelted drop was not collected after the delay");
            helper.assertItemEntityNotPresent(Items.NETHERITE_SCRAP, DROP_POS, 2.0);
        } finally {
            clearDrops(helper, player);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
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

    private static void killWithPlayerCredit(GameTestHelper helper, ServerPlayer player, EntityType<? extends LivingEntity> type) {
        LivingEntity entity = helper.spawn(type, DROP_POS);
        if (!entity.hurtServer(helper.getLevel(), entity.damageSources().playerAttack(player), Float.MAX_VALUE)) {
            fail(helper, "the test player could not kill " + type);
        }
    }

    private static dev.nyon.magnetic.config.Identifier ignoreEntry(net.minecraft.resources.Identifier id) {
        return new dev.nyon.magnetic.config.Identifier(id, false);
    }

    private static void clearDrops(GameTestHelper helper, ServerPlayer player) {
        helper.despawnItem(DROP_POS, 2.0);
        helper.killAllEntitiesOfClass(ExperienceOrb.class);
        player.getInventory().clearContent();
        resetExperience(player);
    }

    private static void resetExperience(ServerPlayer player) {
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0;
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
