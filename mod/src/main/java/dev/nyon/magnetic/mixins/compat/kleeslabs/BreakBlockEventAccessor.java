package dev.nyon.magnetic.mixins.compat.kleeslabs;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.blay09.mods.balm.api.event.BreakBlockEvent")
public interface BreakBlockEventAccessor {

    @Invoker(value = "getPlayer", remap = false)
    Player magnetic$getPlayer();
}
