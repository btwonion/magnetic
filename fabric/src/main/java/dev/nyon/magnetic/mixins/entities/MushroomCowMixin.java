package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import dev.nyon.magnetic.utils.ShearableMixinHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

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
        method = "shear",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/MushroomCow;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"
        )
    )
    private void changeOriginalDropConsumer(
        Args args,
        ServerLevel world,
        SoundSource soundCategory,
        ItemStack stack
    ) {
        args.set(
            2, (ConversionParams.AfterConversion<MushroomCow>) cow -> {
                // Check in the original function for correct usage every version
                world.sendParticles(
                    ParticleTypes.EXPLOSION,
                    cow.getX(),
                    cow.getY(0.5),
                    cow.getZ(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
                );
                cow.dropFromShearingLootTable(
                    world, BuiltInLootTables.SHEAR_MOOSHROOM, stack, (level, dropStack) -> {
                        ServerPlayer player = threadLocal.get();
                        if (player == null || MixinHelper.wrapWithConditionPlayerItemSingle(threadLocal.get(), dropStack)) {
                            for (int i = 0; i < dropStack.getCount(); i++) {
                                level.addFreshEntity(new ItemEntity(
                                    cow.level(),
                                    cow.getX(),
                                    cow.getY(1.0),
                                    cow.getZ(),
                                    dropStack.copyWithCount(1)
                                ));
                            }
                        }
                    }
                );
            }
        );
    }
}
