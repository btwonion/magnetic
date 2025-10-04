package dev.nyon.magnetic.mixins.items;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Painting.class)
public class PaintingMixin {

    @WrapWithCondition(
        method = "dropItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/Painting;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private boolean modifyPaintDrop(
        Painting instance,
        ServerLevel serverLevel,
        ItemLike itemLike,
        ServerLevel world,
        @Nullable Entity entity
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return true;
        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, new ItemStack(itemLike), instance.blockPosition());
    }
}
