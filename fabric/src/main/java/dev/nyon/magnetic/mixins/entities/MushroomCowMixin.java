package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.ShearableMixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.BiConsumer;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;

@Mixin(MushroomCow.class)
public class MushroomCowMixin {

    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/MushroomCow;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void prepareThreadLocalForShearing(
        MushroomCow instance,
        ServerLevel world,
        SoundSource source,
        ItemStack stack,
        Operation<Void> original,
        Player player,
        InteractionHand hand
    ) {
        ShearableMixinHelper.prepare(player, original, instance, world, source, stack);
    }

    @ModifyArgs(
        method = "method_63648",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/MushroomCow;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/BiConsumer;)V"
        )
    )
    private void redirectMushroom(Args args) {
        ServerPlayer serverPlayer = threadLocal.get();
        if (serverPlayer == null) return;
        BiConsumer<ServerLevel, ItemStack> original = args.get(3);

        args.set(
            3, (BiConsumer<ServerLevel, ItemStack>) (world, stack) -> {
                if (MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, stack)) original.accept(world, stack);
            }
        );
    }
}
