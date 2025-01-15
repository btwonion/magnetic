package dev.nyon.magnetic.mixins;

import dev.nyon.magnetic.DropEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(SpawnerBlock.class)
public class SpawnerBlockMixin {

    @ModifyArgs(
        method = "spawnAfterBreak",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/SpawnerBlock;popExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)V"
        )
    )
    private void modifyDroppedExp(
        Args args,
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        ItemStack stack,
        boolean dropExperience
    ) {
        ServerPlayer player = threadLocal.get();
        if (player == null) return;

        MutableInt mutableInt = new MutableInt((int) args.get(2));
        DropEvent.INSTANCE.getEvent()
            .invoker()
            .invoke(new ArrayList<>(), mutableInt, player, stack);

        args.set(2, mutableInt.getValue());
    }
}
