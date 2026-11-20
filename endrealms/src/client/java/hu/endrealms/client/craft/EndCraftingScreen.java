package hu.endrealms.client.craft;

import hu.endrealms.craft.EndCrafting;
import hu.endrealms.craft.EndCraftingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The End Crafting Table's window: the 5x5 grid and its result, with every 5x5 recipe listed on the left. Clicking a
 * recipe shows its pattern faintly in the grid and fills the grid from the inventory when everything is at hand.
 */
public class EndCraftingScreen extends AbstractContainerScreen<EndCraftingMenu> {
	private static final int PANEL = 0xFF17102E;
	private static final int BORDER = 0xFF5B3FA0;
	private static final int SLOT_BG = 0xFF2A2147;
	private static final int SLOT_EDGE = 0xFF4A3A78;
	private static final int TEXT = 0xFFE4DEF5;
	private static final int ACCENT = 0xFF7FE0E8;
	private static final int MUTED = 0xFF9A90B8;
	private static final int LIST_W = 112;
	private static final int CELL = 20;
	private static final int COLUMNS = 5;
	private static final int LIST_TOP = 18;
	private static final int ROWS = 9;

	private static List<EndCrafting.Entry> recipes = List.of();
	private int selected = -1;
	private int scroll;

	public EndCraftingScreen(EndCraftingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 206);
		this.titleLabelY = 6;
		this.inventoryLabelY = EndCraftingMenu.INVENTORY_Y - 11;
	}

	public static void accept(EndCrafting.Recipes payload) {
		recipes = List.copyOf(payload.entries());
	}

	@Override
	protected void init() {
		super.init();
		leftPos += (LIST_W + 4) / 2;          // the window and its recipe list centred together
	}

	private int listX() {
		return leftPos - LIST_W - 4;
	}

	private int maxScroll() {
		return Math.max(0, (recipes.size() + COLUMNS - 1) / COLUMNS - ROWS);
	}

	private int indexAt(double x, double y) {
		int lx = listX() + 6;
		int ly = topPos + LIST_TOP;
		if (x < lx || x >= lx + COLUMNS * CELL || y < ly || y >= ly + ROWS * CELL) {
			return -1;
		}
		int index = (scroll + (int) (y - ly) / CELL) * COLUMNS + (int) (x - lx) / CELL;
		return index < recipes.size() ? index : -1;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractBackground(g, mouseX, mouseY, partial);
		g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
		g.outline(leftPos, topPos, imageWidth, imageHeight, BORDER);
		for (Slot slot : menu.slots) {
			int x = leftPos + slot.x - 1;
			int y = topPos + slot.y - 1;
			int size = slot.index == 0 ? 26 : 18;
			int off = slot.index == 0 ? 4 : 0;
			g.fill(x - off, y - off, x - off + size, y - off + size, SLOT_BG);
			g.outline(x - off, y - off, size, size, slot.index == 0 ? ACCENT : SLOT_EDGE);
		}
		g.text(font, "→", leftPos + EndCraftingMenu.RESULT_X - 22, topPos + EndCraftingMenu.RESULT_Y + 4, TEXT);

		int lx = listX();
		g.fill(lx, topPos, lx + LIST_W, topPos + imageHeight, PANEL);
		g.outline(lx, topPos, LIST_W, imageHeight, BORDER);
		g.text(font, Component.translatable("container.endrealms.end_crafting_table.recipes"), lx + 6, topPos + 6, ACCENT);
		if (recipes.isEmpty()) {
			g.text(font, Component.translatable("container.endrealms.end_crafting_table.none"), lx + 6, topPos + LIST_TOP + 2, MUTED);
		}
		scroll = Mth.clamp(scroll, 0, maxScroll());
		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLUMNS; col++) {
				int index = (scroll + row) * COLUMNS + col;
				if (index >= recipes.size()) {
					continue;
				}
				int x = lx + 6 + col * CELL;
				int y = topPos + LIST_TOP + row * CELL;
				boolean hover = indexAt(mouseX, mouseY) == index;
				g.fill(x, y, x + CELL - 2, y + CELL - 2, index == selected ? 0xFF3F2F78 : hover ? 0xFF33285E : SLOT_BG);
				g.outline(x, y, CELL - 2, CELL - 2, index == selected ? ACCENT : SLOT_EDGE);
				g.item(recipes.get(index).result(), x + 1, y + 1);
			}
		}
		if (maxScroll() > 0) {
			int track = ROWS * CELL;
			int knob = Math.max(10, track * ROWS / (ROWS + maxScroll()));
			int ky = topPos + LIST_TOP + (track - knob) * scroll / maxScroll();
			g.fill(lx + LIST_W - 5, ky, lx + LIST_W - 3, ky + knob, SLOT_EDGE);
		}
		g.text(font, Component.translatable("container.endrealms.end_crafting_table.hint"), lx + 6, topPos + LIST_TOP + ROWS * CELL + 4, MUTED);
	}

	/** The chosen recipe, drawn faintly into the empty cells of the grid. */
	@Override
	public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractContents(g, mouseX, mouseY, partial);
		if (selected < 0 || selected >= recipes.size()) {
			return;
		}
		List<String> cells = recipes.get(selected).cells();
		for (int i = 0; i < cells.size() && i + 1 < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i + 1);
			if (cells.get(i).isEmpty() || slot.hasItem()) {
				continue;
			}
			ItemStack ghost = cycle(cells.get(i));
			int x = leftPos + slot.x;
			int y = topPos + slot.y;
			g.item(ghost, x, y);
			g.fill(x, y, x + 16, y + 16, 0x9917102E);
			if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
				g.setTooltipForNextFrame(font, ghost, mouseX, mouseY);
			}
		}
	}

	private static ItemStack cycle(String options) {
		List<String> ids = new ArrayList<>(List.of(options.split(",")));
		String id = ids.get((int) (System.currentTimeMillis() / 1000 % ids.size()));
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, titleLabelX, titleLabelY, ACCENT);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		int index = indexAt(mouseX, mouseY);
		if (index >= 0) {
			g.setTooltipForNextFrame(font, recipes.get(index).result(), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		int index = indexAt(event.x(), event.y());
		if (index >= 0) {
			selected = index;
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
			return true;
		}
		return super.mouseClicked(event, doubled);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		if (x >= listX() && x < leftPos) {
			scroll = Mth.clamp(scroll - (int) Math.signum(dy), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
	}

	/** Clicks on the recipe list are not clicks outside the window: they must not throw the held item away. */
	@Override
	protected boolean hasClickedOutside(double x, double y, int left, int top) {
		if (x >= listX() && x < leftPos && y >= topPos && y < topPos + imageHeight) {
			return false;
		}
		return super.hasClickedOutside(x, y, left, top);
	}
}
