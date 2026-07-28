package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.BlockDropScope;
import dev.nyon.magnetic.utils.PositionTracker;
import dev.nyon.magnetic.utils.ThreadLocalScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(Level.class)
public class LevelMixin {

    @Unique
    private Level instance = (Level) (Object) this;

    @WrapMethod(method = "destroyBlock")
    private boolean scopeDestroyBlock(
        BlockPos pos,
        boolean dropBlock,
        @Nullable Entity entity,
        int maxUpdateDepth,
        Operation<Boolean> original
    ) {
        if (!(instance instanceof ServerLevel serverLevel)) {
            return original.call(pos, dropBlock, entity, maxUpdateDepth);
        }

        ServerPlayer player = threadLocal.get();
        if (player == null) {
            player = entity instanceof ServerPlayer serverPlayer
                ? serverPlayer
                : ((ServerLevelHolder) serverLevel).getPositionTracker().lookup(pos);
        }
        if (player == null) {
            return original.call(pos, dropBlock, entity, maxUpdateDepth);
        }

        BlockState state = instance.getBlockState(pos);
        if (threadLocal.get() != null) {
            return BlockDropScope.call(
                state,
                () -> original.call(pos, dropBlock, entity, maxUpdateDepth)
            );
        }

        ServerPlayer scopedPlayer = player;
        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        tracker.recordNeighbors(pos, scopedPlayer, serverLevel);
        return ThreadLocalScope.call(
            threadLocal,
            scopedPlayer,
            () -> BlockDropScope.call(
                state,
                () -> original.call(pos, dropBlock, entity, maxUpdateDepth)
            )
        );
    }
}
