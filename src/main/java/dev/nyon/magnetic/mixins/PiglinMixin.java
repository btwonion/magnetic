package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Piglin.class)
public class PiglinMixin {

    @ModifyExpressionValue(
        method = "dropCustomDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/SimpleContainer;removeAllItems()Ljava/util/List;"
        )
    )
    public List<ItemStack> redirectDrops(
        List<ItemStack> original,
        ServerLevel serverLevel, DamageSource damageSource, boolean bl
    ) {
        return MixinHelper.entityCustomDeathLootMultiple(damageSource, original);
    }
}
