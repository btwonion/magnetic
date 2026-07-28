package dev.nyon.magnetic.mixins.compat.kleeslabs;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.blay09.mods.balm.api.event.BreakBlockEvent")
public interface BreakBlockEventAccessor {

    @Invoker(value = "getPlayer", remap = false)
    Player magnetic$getPlayer();

    @Invoker(value = "getState", remap = false)
    BlockState magnetic$getState();
}
