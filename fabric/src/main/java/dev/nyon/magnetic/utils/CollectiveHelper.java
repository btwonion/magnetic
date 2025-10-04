package dev.nyon.magnetic.utils;

import dev.nyon.magnetic.DropEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;

public class CollectiveHelper {

    public static void dropBlock(
        BlockState state,
        ServerLevel serverLevel,
        BlockPos blockPos,
        BlockEntity blockEntity,
        ServerPlayer serverPlayer
    ) {
        ArrayList<ItemStack> drops = new ArrayList<>(Block.getDrops(state, serverLevel, blockPos, blockEntity));

        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(drops, new MutableInt(0), serverPlayer, blockPos);

        drops.forEach(item -> Block.popResource(serverLevel, blockPos, item));
        state.spawnAfterBreak(serverLevel, blockPos, ItemStack.EMPTY, true);
        serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
    }
}
