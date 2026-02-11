package dev.nyon.magnetic.mixins.blocks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.WrapOperationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(Containers.class)
public class ContainersMixin {

    @WrapOperation(
        method = "dropItemStack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private static boolean applyMagneticToContainers(
        Level instance,
        Entity entity,
        Operation<Boolean> original,
        Level world,
        double x,
        double y,
        double z,
        ItemStack stack
    ) {
        if (!(entity instanceof ItemEntity itemEntity)) return original.call(instance, entity);
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return original.call(instance, entity);
        var result = WrapOperationHelper.wrapOperationPlayerItemSingle(
            serverPlayer,
            itemEntity.getItem(),
            new BlockPos((int) x, (int) y, (int) z),
            () -> original.call(instance, entity)
        );

        if (result == null) return false;
        return result;
    }
}
