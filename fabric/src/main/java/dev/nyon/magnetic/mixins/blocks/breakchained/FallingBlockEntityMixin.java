package dev.nyon.magnetic.mixins.blocks.breakchained;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.BreakChainedPlayerHolder;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {

    @WrapWithCondition(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private boolean redirectBlockDrops(
        FallingBlockEntity instance,
        ServerLevel serverLevel,
        ItemLike itemLike
    ) {
        ServerPlayer initialBreaker = ((BreakChainedPlayerHolder) instance.getBlockState()
            .getBlock()).getInitialBreaker();
        return MixinHelper.wrapWithConditionPlayerItemSingle(
            initialBreaker,
            itemLike.asItem().getDefaultInstance()
        );
    }
}
