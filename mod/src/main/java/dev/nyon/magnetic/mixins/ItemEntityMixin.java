package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.Animation;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(
        method = "playerTouch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"
        )
    )
    private void invokeItemEntityTake(
        Player player,
        CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer)) return;
        Animation.INSTANCE.onPickup((ItemEntity) (Object) this);
    }

    /**
     * Records how many items a direct Magnetic pickup actually inserted. Vanilla's own count is unreliable for
     * partial pickups: depending on the version it reports the whole stack or nothing at all.
     */
    @WrapOperation(
        method = "playerTouch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"
        )
    )
    private boolean measureDirectPickup(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        MutableInt inserted = MixinHelper.directPickup.get();
        if (inserted == null) return original.call(inventory, stack);
        int before = stack.getCount();
        boolean result = original.call(inventory, stack);
        inserted.add(before - stack.getCount());
        return result;
    }

    /** DropEvent awards the measured amount for direct pickups instead. */
    @WrapWithCondition(
        method = "playerTouch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;I)V"
        )
    )
    private boolean skipDirectPickupStat(Player player, Stat<?> stat, int amount) {
        return MixinHelper.directPickup.get() == null;
    }
}
