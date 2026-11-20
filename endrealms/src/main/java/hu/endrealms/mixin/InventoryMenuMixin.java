package hu.endrealms.mixin;

import hu.endrealms.bag.Bags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Appends the Backpack's slots after the vanilla ones, whose indexes the game relies on. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
	private InventoryMenuMixin(MenuType<?> type, int id) {
		super(type, id);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void endrealms$addBagSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
		SimpleContainer bag = Bags.of(owner);
		addSlot(new Bags.WornSlot(bag));
		for (int row = 0; row < Bags.ROWS; row++) {
			addSlot(new Bags.StorageSlot(bag, row));
		}
	}
}
