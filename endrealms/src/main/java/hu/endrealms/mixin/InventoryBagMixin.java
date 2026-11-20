package hu.endrealms.mixin;

import hu.endrealms.bag.Bags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Whatever no longer fits in the inventory goes into a worn Backpack, so it counts when picking things up. */
@Mixin(Inventory.class)
public abstract class InventoryBagMixin {
	@Shadow
	@Final
	public Player player;

	@Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
	private void endrealms$overflowIntoBag(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (slot != -1 || stack.isEmpty() || player.level().isClientSide()) {
			return;
		}
		SimpleContainer bag = Bags.of(player);
		if (!Bags.worn(bag)) {
			return;
		}
		int before = stack.getCount();
		ItemStack rest = bag.addItem(stack.copy());
		stack.setCount(rest.getCount());
		if (stack.getCount() < before) {
			cir.setReturnValue(true);
		}
	}
}
