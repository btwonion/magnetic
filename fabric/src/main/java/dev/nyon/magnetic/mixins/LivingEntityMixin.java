package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.nyon.magnetic.DropEvent;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyExpressionValue(
        method = "dropExperience",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getExperienceReward(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)I"
        )
    )
    public int redirectExp(
        int original,
        ServerLevel level,
        Entity entity
    ) {
        if (!(entity instanceof ServerPlayer player)) return original;

        return MixinHelper.modifyExpressionValuePlayerExp(player, original);
    }

    @Unique
    private Consumer<ItemStack> replaceConsumer(
        LootParams params,
        Consumer<ItemStack> original
    ) {
        DamageSource source = params.contextMap()
            .getOptional(LootContextParams.DAMAGE_SOURCE);
        if (source == null || !(source.getEntity() instanceof ServerPlayer player)) return original;

        return item -> {
            ArrayList<ItemStack> mutableList = new ArrayList<>(List.of(item));
            DropEvent.INSTANCE.getEvent()
                .invoker()
                .invoke(mutableList, new MutableInt(0), player);

            if (!mutableList.isEmpty()) original.accept(item);
        };
    }

    @ModifyArg(
        method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"
        ),
        index = 2
    )
    public Consumer<ItemStack> redirectCommonDrops(
        LootParams params,
        long seed,
        Consumer<ItemStack> original
    ) {
        return replaceConsumer(params, original);
    }

    @ModifyArg(
        method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Function;Ljava/util/function/BiConsumer;)Z",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"
        )
    )
    public Consumer<ItemStack> redirectDropConsumer(
        Consumer<ItemStack> original,
        @Local(ordinal = 0)
        LootParams params
    ) {
        return replaceConsumer(params, original);
    }
}