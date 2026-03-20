package dev.nyon.magnetic.mixins;

import dev.nyon.magnetic.DropEvent;
import dev.nyon.magnetic.holders.ServerLevelHolder;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.PositionTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static dev.nyon.magnetic.utils.MixinHelper.animationSkip;
import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements ServerLevelHolder {

    @Unique
    private final PositionTracker magnetic$positionTracker = new PositionTracker();

    @Override
    public @NonNull PositionTracker getPositionTracker() {
        return magnetic$positionTracker;
    }

    // Tick cleanup for position tracker
    @Inject(
        method = "tick",
        at = @At("TAIL")
    )
    private void cleanupPositionTracker(CallbackInfo ci) {
        magnetic$positionTracker.cleanup();
    }

    // Central interception: addFreshEntity
    @Inject(
        method = "addFreshEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void interceptEntity(
        Entity entity,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (animationSkip.get()) return;

        ServerPlayer player = threadLocal.get();
        if (player == null) {
            player = magnetic$positionTracker.lookup(entity.blockPosition());
        }
        if (player == null) return;

        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) return;

            ArrayList<ItemStack> items = new ArrayList<>(List.of(stack.copy()));
            DropEvent.INSTANCE.getEvent()
                .invoker()
                .invoke(items, new MutableInt(0), player, entity.blockPosition());

            if (items.isEmpty()) {
                cir.setReturnValue(false);
            } else {
                itemEntity.setItem(items.getFirst());
            }
        } else if (entity instanceof ExperienceOrb orb) {
            int value = ((ExperienceOrbInvoker) orb).invokeGetValue();
            if (value <= 0) return;

            int remaining = MixinHelper.modifyExpressionValuePlayerExp(player, value, entity.blockPosition());
            if (remaining <= 0) {
                cir.setReturnValue(false);
            } else {
                ((ExperienceOrbInvoker) orb).invokeSetValue(remaining);
            }
        }
    }
}
