package dev.nyon.magnetic.mixins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
/*? if <1.21.11 {*/
/*import org.spongepowered.asm.mixin.gen.Accessor;
*//*?}*/
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExperienceOrb.class)
public interface ExperienceOrbInvoker {

    @Invoker("repairPlayerItems")
    int invokeRepairPlayerItems(
        ServerPlayer serverPlayer,
        int i
    );

    @Invoker("getValue")
    int invokeGetValue();

    /*? if >=1.21.11 {*/
    @Invoker("setValue")
    /*?} else {*/
    /*@Accessor("value")
    *//*?}*/
    void invokeSetValue(int value);
}
