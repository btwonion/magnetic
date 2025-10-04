package dev.nyon.magnetic.mixins.items;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import dev.nyon.magnetic.DropEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Shadow
    private @Nullable Entity hookedIn;
    @Shadow
    @Final
    private RandomSource syncronizedRandom;

    @ModifyReceiver(
        method = "retrieve",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;iterator()Ljava/util/Iterator;"
        )
    )
    public List<ItemStack> modifyFishingDrops(
        List<ItemStack> instance,
        ItemStack fishingHook
    ) {
        if (!(getPlayerOwner() instanceof ServerPlayer player)) return instance;

        ArrayList<ItemStack> mutableList = new ArrayList<>(instance);
        mutableList.removeIf(item -> {
            ArrayList<ItemStack> singleList = new ArrayList<>(List.of(item));
            DropEvent.INSTANCE.getEvent()
                .invoker()
                .invoke(singleList, new MutableInt(syncronizedRandom.nextInt(6) + 1), player, hookedIn.blockPosition());
            return singleList.isEmpty();
        });

        return mutableList;
    }

    @Shadow
    @Nullable
    public abstract Player getPlayerOwner();
}
