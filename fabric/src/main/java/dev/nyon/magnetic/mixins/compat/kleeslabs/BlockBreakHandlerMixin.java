package dev.nyon.magnetic.mixins.compat.kleeslabs;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.blay09.mods.balm.api.event.BreakBlockEvent;
import net.blay09.mods.kleeslabs.BlockBreakHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBreakHandler.class)
public class BlockBreakHandlerMixin {

    @WrapWithCondition(
        method = "onBreakBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private static boolean modifyDroppedSlab(
        Level instance,
        Entity entity,
        BreakBlockEvent event
    ) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer) || !(event.getLevel() instanceof ServerLevel)) return true;
        if (!(entity instanceof ItemEntity itemEntity)) return true;
        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, itemEntity.getItem(), event.getPos());
    }
}
