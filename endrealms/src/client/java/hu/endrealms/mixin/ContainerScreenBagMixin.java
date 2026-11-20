package hu.endrealms.mixin;

import hu.endrealms.bag.Bags;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Backpack panel lies below the window, and the game checks for a click outside the window when the mouse button
 * is released too, even over a slot: without this, releasing over a Backpack slot would throw the held item away.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class ContainerScreenBagMixin {
	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void endrealms$panelIsInside(double x, double y, int left, int top, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof InventoryScreen screen && screen.getMenu().getSlot(Bags.MENU_SLOT).hasItem()
				&& x >= left && x < left + 176 && y >= top + Bags.ROW_Y - 7 && y < top + Bags.ROW_Y + 23) {
			cir.setReturnValue(false);
		}
	}
}
