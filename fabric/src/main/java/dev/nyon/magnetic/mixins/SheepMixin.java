package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.DropEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(Sheep.class)
public abstract class SheepMixin {

    @Unique
    private Sheep instance = (Sheep) (Object) this;

    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Sheep;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void prepareThreadLocalForShearing(
        Sheep instance,
        ServerLevel world,
        SoundSource source,
        ItemStack stack,
        Operation<Void> original,
        Player player,
        InteractionHand hand
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            original.call(instance, world, source, stack);
            return;
        }

        ServerPlayer previous = threadLocal.get();
        threadLocal.set(serverPlayer);
        try {
            original.call(instance, world, source, stack);
        } finally {
            threadLocal.set(previous);
        }
    }

    @ModifyArg(
        method = "shear",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Sheep;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/BiConsumer;)V"
        ),
        index = 3
    )
    private BiConsumer<ServerLevel, ItemStack> changeOriginalDropConsumer(BiConsumer<ServerLevel, ItemStack> original) {
        DamageSource source = instance.getLastDamageSource();
        if (source == null || !(source.getEntity() instanceof ServerPlayer player)) return original;

        return (world, item) -> {
            ArrayList<ItemStack> mutableList = new ArrayList<>(List.of(item));
            DropEvent.INSTANCE.getEvent()
                .invoker()
                .invoke(mutableList, new MutableInt(0), player);

            if (!mutableList.isEmpty()) original.accept(world, item);
        };
    }
}