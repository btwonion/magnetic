package dev.nyon.magnetic.mixins.entities;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.BiConsumer;

@Mixin(Armadillo.class)
public class ArmadilloMixin {

    @Unique
    private Armadillo instance = (Armadillo) (Object) this;

    @ModifyArgs(
        method = "brushOffScute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/armadillo/Armadillo;dropFromEntityInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/BiConsumer;)Z"
        )
    )
    private void modifyScuteDrop(
        Args args,
        @Nullable Entity entity,
        ItemStack stack
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return;
        BiConsumer<ServerLevel, ItemStack> original = args.get(5);
        args.set(
            5, (BiConsumer<ServerLevel, ItemStack>) (level, item) -> {
                if (MixinHelper.entityWrapWithConditionPlayerItemSingle(
                    serverPlayer,
                    stack,
                    instance,
                    instance.blockPosition()
                )) original.accept(level, item);
            }
        );
    }
}
