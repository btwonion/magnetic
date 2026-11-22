package dev.nyon.magnetic.mixins.fluids;

import dev.nyon.magnetic.config.ConfigKt;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.PositionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
/*? if >=1.21.11 {*/
import net.minecraft.world.entity.LivingEntity;
/*?} else {*/
/*import net.minecraft.world.entity.player.Player;
*//*?}*/
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records which player placed a fluid so drops washed away by the spreading fluid can be attributed to them.
 *
 * <p>The hook sits on {@code playEmptySound} instead of {@code emptyContents}: NeoForge adds an
 * ItemStack-aware {@code emptyContents} overload and turns the vanilla one into a delegate, so a
 * Fabric-built injection into {@code emptyContents} finds no target when the Fabric jar runs through
 * Sinytra Connector and aborts startup. {@code playEmptySound} has the same descriptor on every
 * loader and is called after the fluid has been placed. {@link MobBucketItem} overrides it without
 * calling super, so it is targeted separately.
 */
@Mixin({BucketItem.class, MobBucketItem.class})
public class BucketItemMixin {

    @Inject(
        method = "playEmptySound",
        at = @At("HEAD")
    )
    private void recordBucketPlacement(
        /*? if >=1.21.11 {*/ LivingEntity /*?} else {*/ /*Player *//*?}*/ user,
        LevelAccessor level,
        BlockPos pos,
        CallbackInfo ci
    ) {
        if (!(user instanceof ServerPlayer serverPlayer)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!ConfigKt.getConfig()
            .getBuckets()
            .getEnabled()) return;
        // Mob buckets without a fluid only release the mob, so there is no flow to attribute.
        if (serverLevel.getFluidState(pos).isEmpty()) return;

        PositionTracker tracker = ((ServerLevelHolder) serverLevel).getPositionTracker();
        tracker.record(
            pos,
            serverPlayer,
            ConfigKt.getConfig()
                .getBuckets()
                .getAbilityTimeout()
        );
    }
}
