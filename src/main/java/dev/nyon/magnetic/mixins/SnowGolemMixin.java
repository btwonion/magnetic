package dev.nyon.magnetic.mixins;

/*? if <1.21.2 {*/
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static dev.nyon.magnetic.utils.MixinHelper.threadLocal;
/*?}*/
import net.minecraft.world.entity.animal.SnowGolem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SnowGolem.class)
public class SnowGolemMixin {

    /*? if <1.21.2 {*/
    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/SnowGolem;shear(Lnet/minecraft/sounds/SoundSource;)V"
        )
    )
    private void prepareThreadLocalForShearing(
        SnowGolem instance,
        SoundSource source,
        Operation<Void> original,
        Player _player
    ) {
        MixinHelper.prepareShearableServerPlayer(instance, source, original, _player);
    }

    @ModifyArg(
        method = "shear",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;<init>(Lnet/minecraft/world/level/ItemLike;)V"
        )
    )
    private ItemLike modifyShearDrops(ItemLike original) {
        ServerPlayer player = threadLocal.get();
        if (player == null) return original;

        if (MixinHelper.wrapWithConditionPlayerItemSingle(player, new ItemStack(original))) return original;
        else return Items.AIR;
    }
    /*?}*/
}
