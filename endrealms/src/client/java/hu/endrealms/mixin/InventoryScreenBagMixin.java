package hu.endrealms.mixin;

import hu.endrealms.bag.Bags;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Backpack's slot above the off hand and, while one is worn, the panel with its nine slots under the inventory. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenBagMixin {
	@Unique
	private static final int PANEL = 0xFFC6C6C6;
	@Unique
	private static final int DARK = 0xFF373737;
	@Unique
	private static final int SLOT = 0xFF8B8B8B;
	@Unique
	private static final int LIGHT = 0xFFFFFFFF;
	@Unique
	private static final int HINT = 0xFF6F6F6F;

	@Unique
	private static void endrealms$slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
		g.fill(x - 1, y - 1, x + 17, y, DARK);
		g.fill(x - 1, y - 1, x, y + 17, DARK);
		g.fill(x - 1, y + 16, x + 17, y + 17, LIGHT);
		g.fill(x + 16, y - 1, x + 17, y + 17, LIGHT);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void endrealms$drawBag(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		ContainerScreenAccessor screen = (ContainerScreenAccessor) this;
		int left = screen.endrealms$left();
		int top = screen.endrealms$top();
		int x = left + Bags.WORN_X;
		int y = top + Bags.WORN_Y;
		endrealms$slot(g, x, y);
		boolean worn = ((AbstractContainerScreen<?>) (Object) this).getMenu().getSlot(Bags.MENU_SLOT).hasItem();
		if (!worn) {
			g.fill(x + 4, y + 6, x + 12, y + 14, HINT);       // a faint bag where the empty slot is
			g.fill(x + 6, y + 3, x + 10, y + 4, HINT);
			g.fill(x + 5, y + 4, x + 6, y + 6, HINT);
			g.fill(x + 10, y + 4, x + 11, y + 6, HINT);
			return;
		}
		int panelTop = top + Bags.ROW_Y - 7;
		int panelBottom = top + Bags.ROW_Y + 16 + 7;
		g.fill(left, panelTop, left + 176, panelBottom, DARK);
		g.fill(left + 1, panelTop + 1, left + 175, panelBottom - 1, LIGHT);
		g.fill(left + 3, panelTop + 3, left + 175, panelBottom - 1, 0xFF555555);
		g.fill(left + 3, panelTop + 3, left + 174, panelBottom - 2, PANEL);
		for (int row = 0; row < Bags.ROWS; row++) {
			endrealms$slot(g, left + Bags.ROW_X + row * 18, top + Bags.ROW_Y);
		}
	}
}
