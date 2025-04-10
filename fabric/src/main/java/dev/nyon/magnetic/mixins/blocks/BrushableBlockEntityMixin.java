package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BrushableBlockEntity.class)
public class BrushableBlockEntityMixin {

    @WrapWithCondition(
        method = "dropContent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean useMagneticInsteadOfDrop(
        ServerLevel instance,
        Entity entity,
        ServerLevel world,
        LivingEntity livingEntity,
        ItemStack stack
    ) {
        if (!(livingEntity instanceof ServerPlayer serverPlayer)) return true;
        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, ((ItemEntity) entity).getItem());
    }
}
