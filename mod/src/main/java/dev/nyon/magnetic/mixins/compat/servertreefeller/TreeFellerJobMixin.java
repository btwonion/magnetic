package dev.nyon.magnetic.mixins.compat.servertreefeller;

import dev.nyon.magnetic.holders.TreeFellerJobHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Pseudo
@Mixin(targets = "com.mcstack.treefeller.TreeFeller$Job")
public class TreeFellerJobMixin implements TreeFellerJobHolder {

    @Unique
    private @Nullable UUID magnetic$player;

    @Override
    public @Nullable UUID getMagneticPlayer() {
        return magnetic$player;
    }

    @Override
    public void setMagneticPlayer(@Nullable UUID player) {
        magnetic$player = player;
    }
}
