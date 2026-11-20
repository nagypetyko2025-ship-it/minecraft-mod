package hu.endrealms.client.gem;

import hu.endrealms.gem.GemTableMenu;
import hu.endrealms.gem.Gems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Gem Table's window; above the slots it spells out what the chosen gem would give to the chosen item. */
public class GemTableScreen extends AbstractContainerScreen<GemTableMenu> {
	private static final int PANEL = 0xFF17102E;
	private static final int BORDER = 0xFF5B3FA0;
	private static final int SLOT_BG = 0xFF2A2147;
	private static final int SLOT_EDGE = 0xFF4A3A78;
	private static final int TEXT = 0xFFE4DEF5;
	private static final int ACCENT = 0xFF7FE0E8;
	private static final int WARN = 0xFFF2A07A;

	public GemTableScreen(GemTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		this.titleLabelY = 6;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractBackground(g, mouseX, mouseY, partial);
		g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
		g.outline(leftPos, topPos, imageWidth, imageHeight, BORDER);
		for (Slot slot : menu.slots) {
			int x = leftPos + slot.x - 1;
			int y = topPos + slot.y - 1;
			g.fill(x, y, x + 18, y + 18, SLOT_BG);
			g.outline(x, y, 18, 18, SLOT_EDGE);
		}
		int y = topPos + GemTableMenu.SLOT_Y + 4;
		g.text(font, "+", leftPos + 58, y, TEXT);
		g.text(font, "→", leftPos + 108, y, TEXT);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, titleLabelX, titleLabelY, ACCENT);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT);
		ItemStack item = menu.item();
		if (menu.removing()) {
			wrapped(g, Component.translatable("gem.endrealms.remove"), WARN);
			return;
		}
		Gems.Gem gem = Gems.gemOf(menu.gem());
		Gems.Category category = Gems.categoryOf(item);
		if (gem == null || category == null) {
			wrapped(g, Component.translatable("gem.endrealms.hint"), 0xFF9A90B8);
			return;
		}
		Component problem = Gems.problem(item, gem);
		if (problem != null) {
			wrapped(g, problem, WARN);
			return;
		}
		int y = 18;
		for (Gems.Bonus bonus : gem.bonuses(category)) {
			g.text(font, bonus.describe(), 8, y, ACCENT);
			y += 11;
		}
	}

	private void wrapped(GuiGraphicsExtractor g, Component text, int color) {
		int y = 18;
		for (FormattedCharSequence row : font.split(text, imageWidth - 16)) {
			g.text(font, row, 8, y, color);
			y += 10;
		}
	}
}
