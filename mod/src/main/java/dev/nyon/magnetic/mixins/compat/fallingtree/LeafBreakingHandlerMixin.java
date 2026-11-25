package dev.nyon.magnetic.mixins.compat.fallingtree;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import fr.rakambda.fallingtree.common.leaf.LeafBreakingHandler;
import fr.rakambda.fallingtree.common.leaf.LeafBreakingSchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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

        if (schedule.getLevel().getRaw() instanceof ServerLevel level
            && schedule.getBlockPos().getRaw() instanceof BlockPos pos) {
            MixinHelper.recordLeafDecay(level, pos);
        }
    }
}
