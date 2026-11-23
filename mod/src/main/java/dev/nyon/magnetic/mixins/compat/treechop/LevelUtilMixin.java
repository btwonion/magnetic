package dev.nyon.magnetic.mixins.compat.treechop;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

/**
 * HT's TreeChop harvests every felled log and leaf through this helper. Scope
 * each harvested block individually so ignored blocks keep dropping normally,
 * independent of the log the player originally chopped.
 */
@Pseudo
@Mixin(targets = "ht.treechop.common.util.LevelUtil")
public class LevelUtilMixin {

    @WrapMethod(method = "harvestBlock")
    private static void scopeDropsToPlayer(
        Entity entity,
        Level level,
        BlockPos pos,
        ItemStack tool,
        boolean notifyWillDestroy,
        Operation<Void> original
    ) {
        ServerPlayer player = entity instanceof ServerPlayer serverPlayer ? serverPlayer : threadLocal.get();
        if (player == null || !(level instanceof ServerLevel)) {
            original.call(entity, level, pos, tool, notifyWillDestroy);
            return;
        }

        BlockDropScope.run(
            level.getBlockState(pos),
            () -> ThreadLocalScope.run(
                threadLocal,
                player,
                () -> original.call(entity, level, pos, tool, notifyWillDestroy)
            )
        );
    }
}
