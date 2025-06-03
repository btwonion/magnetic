package dev.nyon.magnetic.mixins.entities;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {

    @WrapWithCondition(
        method = "attemptToShearEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private boolean redirectShearDrops(
        Entity instance,
        ServerLevel serverLevel,
        ItemStack itemStack,
        Vec3 vec3,
        Player player,
        InteractionHand hand,
        ItemStack tool,
        Mob mob
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;

        return MixinHelper.wrapWithConditionPlayerItemSingle(serverPlayer, itemStack);
    }
}
