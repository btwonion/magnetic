package dev.nyon.magnetic.mixins.entities;

import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {

    @Unique
    private WitherBoss instance = (WitherBoss) (Object) this;

    @ModifyArgs(
        method = "dropCustomDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    protected void redirectEquipmentDrop(
        Args args,
        ServerLevel serverLevel,
        DamageSource damageSource,
        boolean bl
    ) {
        ItemLike original = args.get(1);

        if (MixinHelper.entityCustomDeathLootSingle(damageSource, new ItemStack(original), instance, instance.blockPosition())) return;
        args.set(1, Items.AIR);
    }
}
