package dev.nyon.magnetic.mixins.compat.veinminer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Pseudo
@Mixin(targets = "de.miraculixx.veinminer.event.VeinMinerEvent")
public class VeinMinerEventMixin {

    @WrapMethod(method = "improvedDropResources")
    private void scopeDropsToPlayer(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockEntity blockEntity,
        Entity breaker,
        ItemStack tool,
        BlockPos initialSource,
        Operation<Void> original
    ) {
        if (!(breaker instanceof ServerPlayer player)) {
            original.call(state, level, pos, blockEntity, breaker, tool, initialSource);
            return;
        }

        BlockDropScope.run(
            state,
            () -> ThreadLocalScope.run(
                threadLocal,
                player,
                () -> original.call(state, level, pos, blockEntity, breaker, tool, initialSource)
            )
        );
    }
}
