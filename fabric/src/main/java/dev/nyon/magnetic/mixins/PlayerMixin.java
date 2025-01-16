package dev.nyon.magnetic.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nyon.magnetic.utils.MixinHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(Player.class)
public class PlayerMixin {

    @Unique
    final Player instance = (Player) (Object) this;

    @WrapOperation(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;dropAll()V"
        )
    )
    public void modifyEquipmentDrops(
        Inventory inventory,
        Operation<Void> original
    ) {

        for (List<ItemStack> list : inventory.compartments) {
            for (int i = 0; i < list.size(); ++i) {
                ItemStack itemStack = list.get(i);
                if (!itemStack.isEmpty()) {
                    ArrayList<ItemStack> singleItem = new ArrayList<>(List.of(itemStack));
                    List<ItemStack> processedItems = MixinHelper.entityDropEquipmentMultiple(instance, singleItem);
                    if (processedItems.isEmpty()) list.set(i, ItemStack.EMPTY);
                }
            }
        }

        original.call(inventory);
    }
}
