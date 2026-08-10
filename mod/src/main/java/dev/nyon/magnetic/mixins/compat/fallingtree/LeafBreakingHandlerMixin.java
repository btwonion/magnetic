package dev.nyon.magnetic.mixins.compat.fallingtree;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import fr.rakambda.fallingtree.common.leaf.LeafBreakingHandler;
import fr.rakambda.fallingtree.common.leaf.LeafBreakingSchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.ignoreBlockDrops;
import static dev.nyon.magnetic.utils.MixinHelper.leafDecayAuthorizedPlayer;
import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(LeafBreakingHandler.class)
public class LeafBreakingHandlerMixin {

    @WrapOperation(
        method = "onBlockUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lfr/rakambda/fallingtree/common/leaf/LeafBreakingHandler;addSchedule(Lfr/rakambda/fallingtree/common/leaf/LeafBreakingSchedule;)V"
        )
    )
    private void rememberScheduledLeafPlayer(
        LeafBreakingHandler instance,
        LeafBreakingSchedule schedule,
        Operation<Void> original
    ) {
        original.call(instance, schedule);

        ServerPlayer player = threadLocal.get();
        if (player == null
            || !player.getUUID().equals(leafDecayAuthorizedPlayer.get())
            || Boolean.TRUE.equals(ignoreBlockDrops.get())
            || !(schedule.getLevel().getRaw() instanceof ServerLevel level)
            || !(schedule.getBlockPos().getRaw() instanceof BlockPos pos)) {
            return;
        }

        long timeout = ConfigKt.getConfig().getLeafDecay().getAbilityTimeout();
        ((ServerLevelHolder) level).getLeafDecayTracker().record(pos, player.getUUID(), timeout);
    }
}
