package hu.endrealms.client.brew;

import hu.endrealms.brew.Brewing;
import hu.endrealms.brew.FermentationStationBlockEntity;
import hu.endrealms.brew.FermentationStationMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Fermentation Station's window: the water tank, three ingredient slots with how many of each a bottle takes, the
 * bottle, the arrow with the Start / Stop button under it, the brandy, and on the right every recipe.
 */
public class FermentationStationScreen extends AbstractContainerScreen<FermentationStationMenu> {
	private static final int PANEL = 0xFF2A1E14;
	private static final int BORDER = 0xFFB0683E;
	private static final int SLOT_BG = 0xFF3B2A1C;
	private static final int SLOT_EDGE = 0xFF6E4A30;
	private static final int TEXT = 0xFFF3E6D2;
	private static final int MUTED = 0xFFB8A48C;
	private static final int AMBER = 0xFFE8A33A;
	private static final int GOOD = 0xFF8ED46A;
	private static final int BAD = 0xFFE07070;
	private static final int WATER = 0xFF3F76E4;
	private static final int WATER_LIGHT = 0xFF6E9BF0;
	private static final int TANK_X = 8;
	private static final int TANK_Y = 20;
	private static final int TANK_W = 12;
	private static final int TANK_H = 68;
	private static final int ARROW_X = 106;
	private static final int ARROW_W = 30;
	private static final int ARROW_Y = 44;
	private static final int LIST_W = 112;
	private static final int ROW_H = 20;
	private static final int LIST_TOP = 20;
	/** How many recipe rows fit above the note at the bottom; the rest scroll with the mouse wheel. */
	private static final int VISIBLE = (FermentationStationMenu.HEIGHT - LIST_TOP - 26) / ROW_H;

	private int scroll;
	private Brewing.Brew shownCurrent;

	private Button start;

	public FermentationStationScreen(FermentationStationMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, FermentationStationMenu.HEIGHT);
		this.titleLabelY = 6;
		this.inventoryLabelY = FermentationStationMenu.INVENTORY_Y - 11;
	}

	@Override
	protected void init() {
		super.init();
		leftPos -= (LIST_W + 4) / 2;          // the window and its recipe list centred together
		start = addRenderableWidget(Button.builder(Component.translatable("fermentation.endrealms.start"),
				button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FermentationStationMenu.BUTTON_START))
				.bounds(leftPos + 100, topPos + 70, 70, 18).build());
		updateButton();
	}

	private void updateButton() {
		if (start == null) {
			return;
		}
		start.setMessage(Component.translatable(menu.running() ? "fermentation.endrealms.stop" : "fermentation.endrealms.start"));
		start.active = menu.running() || menu.canStart();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		updateButton();
	}

	private int listX() {
		return leftPos + imageWidth + 4;
	}

	/** The recipe row under the mouse, or -1. */
	private int rowAt(double x, double y) {
		int lx = listX();
		int ly = topPos + LIST_TOP;
		if (x < lx + 3 || x >= lx + LIST_W - 3 || y < ly) {
			return -1;
		}
		int row = (int) (y - ly) / ROW_H;
		return row < VISIBLE && row + scroll < Brewing.brews().size() ? row + scroll : -1;
	}

	private int maxScroll() {
		return Math.max(0, Brewing.brews().size() - VISIBLE);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		int lx = listX();
		if (x >= lx && x < lx + LIST_W && y >= topPos && y < topPos + imageHeight && dy != 0) {
			scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
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
		g.outline(leftPos + FermentationStationMenu.OUT_X - 3, topPos + FermentationStationMenu.OUT_Y - 3, 22, 22, AMBER);

		int tx = leftPos + TANK_X;                                         // the water tank, one notch per bucket
		int ty = topPos + TANK_Y;
		g.fill(tx, ty, tx + TANK_W, ty + TANK_H, SLOT_BG);
		g.outline(tx - 1, ty - 1, TANK_W + 2, TANK_H + 2, SLOT_EDGE);
		int water = Math.min(FermentationStationBlockEntity.MAX_WATER, menu.water());
		int filled = TANK_H * water / FermentationStationBlockEntity.MAX_WATER;
		g.fill(tx, ty + TANK_H - filled, tx + TANK_W, ty + TANK_H, WATER);
		if (filled > 0) {
			g.fill(tx, ty + TANK_H - filled, tx + TANK_W, ty + TANK_H - filled + 1, WATER_LIGHT);
		}
		for (int i = 1; i < FermentationStationBlockEntity.MAX_WATER; i++) {
			int notch = ty + TANK_H - TANK_H * i / FermentationStationBlockEntity.MAX_WATER;
			g.fill(tx, notch, tx + 3, notch + 1, SLOT_EDGE);
		}

		int x = leftPos + ARROW_X;                                         // the arrow
		int y = topPos + ARROW_Y;
		g.fill(x, y, x + ARROW_W, y + 8, SLOT_BG);
		for (int i = 0; i < 6; i++) {
			g.fill(x + ARROW_W + i, y - 2 + i, x + ARROW_W + i + 1, y + 10 - i, SLOT_BG);
		}
		int done = Math.round(menu.progress() * (ARROW_W + 6));
		for (int i = 0; i < done; i++) {
			int column = x + i;
			if (i < ARROW_W) {
				g.fill(column, y + 1, column + 1, y + 7, AMBER);
			} else {
				int head = i - ARROW_W;
				g.fill(column, y - 1 + head, column + 1, y + 9 - head, AMBER);
			}
		}

		int lx = listX();                                                  // the recipe list
		g.fill(lx, topPos, lx + LIST_W, topPos + imageHeight, PANEL);
		g.outline(lx, topPos, LIST_W, imageHeight, BORDER);
		g.text(font, Component.translatable("fermentation.endrealms.recipes"), lx + 6, topPos + 6, AMBER);
		Brewing.Brew current = Brewing.intended(menu.ingredients());
		List<Brewing.Brew> brews = Brewing.brews();
		if (current != shownCurrent) {                                     // scroll the recipe just put together into view
			shownCurrent = current;
			int index = current == null ? -1 : brews.indexOf(current);
			if (index >= 0 && (index < scroll || index >= scroll + VISIBLE)) {
				scroll = Math.max(0, Math.min(maxScroll(), index - VISIBLE / 2));
			}
		}
		if (maxScroll() > 0) {                                             // a thin scroll bar on the right edge
			int trackTop = topPos + LIST_TOP;
			int trackH = VISIBLE * ROW_H - 2;
			int thumbH = Math.max(10, trackH * VISIBLE / brews.size());
			int thumbY = trackTop + (trackH - thumbH) * scroll / maxScroll();
			g.fill(lx + LIST_W - 4, trackTop, lx + LIST_W - 2, trackTop + trackH, SLOT_BG);
			g.fill(lx + LIST_W - 4, thumbY, lx + LIST_W - 2, thumbY + thumbH, SLOT_EDGE);
		}
		for (int i = scroll; i < Math.min(brews.size(), scroll + VISIBLE); i++) {
			Brewing.Brew brew = brews.get(i);
			int ry = topPos + LIST_TOP + (i - scroll) * ROW_H;
			boolean on = current == brew;
			if (on || rowAt(mouseX, mouseY) == i) {
				g.fill(lx + 3, ry, lx + LIST_W - 6, ry + ROW_H - 2, on ? 0xFF4A3420 : SLOT_BG);
			}
			if (on) {
				g.outline(lx + 3, ry, LIST_W - 9, ROW_H - 2, AMBER);
			}
			int ix = lx + 6;
			for (Brewing.Input input : brew.inputs()) {
				g.item(new ItemStack(input.item()), ix, ry + 1);
				String count = String.valueOf(input.count());
				g.text(font, count, ix + 17 - font.width(count), ry + 10, TEXT);
				ix += 20;
			}
			g.text(font, "→", lx + LIST_W - 40, ry + 5, MUTED);
			g.item(new ItemStack(brew.brandy()), lx + LIST_W - 24, ry + 1);
		}
		int hy = topPos + LIST_TOP + VISIBLE * ROW_H + 2;
		for (FormattedCharSequence line : font.split(Component.translatable("fermentation.endrealms.each"), LIST_W - 12)) {
			g.text(font, line, lx + 6, hy, MUTED);
			hy += 10;
		}
	}

	/** Over a recipe: the ingredient or the brandy with all it does; over the tank: how much water is in it. */
	@Override
	protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		int row = rowAt(mouseX, mouseY);
		if (row >= 0) {
			Brewing.Brew brew = Brewing.brews().get(row);
			int lx = listX();
			int slot = (mouseX - lx - 6) / 20;
			ItemStack shown = mouseX >= lx + LIST_W - 26 || slot >= brew.inputs().size() ? new ItemStack(brew.brandy())
					: new ItemStack(brew.inputs().get(Math.max(0, slot)).item());
			g.setTooltipForNextFrame(font, shown, mouseX, mouseY);
		}
		int tx = leftPos + TANK_X;
		int ty = topPos + TANK_Y;
		if (mouseX >= tx && mouseX < tx + TANK_W && mouseY >= ty && mouseY < ty + TANK_H) {
			g.setTooltipForNextFrame(font, Component.translatable("fermentation.endrealms.water", menu.water(), FermentationStationBlockEntity.MAX_WATER), mouseX, mouseY);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, titleLabelX, titleLabelY, AMBER);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT);
		String water = menu.water() + "/" + FermentationStationBlockEntity.MAX_WATER;
		g.text(font, water, imageWidth - 8 - font.width(water), titleLabelY, menu.water() > 0 ? WATER_LIGHT : BAD);

		List<ItemStack> ingredients = menu.ingredients();
		Brewing.Brew brew = Brewing.intended(ingredients);
		if (brew != null) {                                                // under each slot: how many a bottle takes
			for (int i = 0; i < ingredients.size(); i++) {
				ItemStack stack = ingredients.get(i);
				for (Brewing.Input input : brew.inputs()) {
					if (!stack.isEmpty() && stack.is(input.item())) {
						String need = "×" + input.count();
						int sx = FermentationStationMenu.IN_X + i * FermentationStationMenu.IN_STEP + 8 - font.width(need) / 2;
						g.text(font, need, sx, FermentationStationMenu.IN_Y + 19, stack.getCount() >= input.count() ? GOOD : BAD);
					}
				}
			}
		}
		int statusY = FermentationStationMenu.IN_Y + 31;
		int room = FermentationStationMenu.OUT_X - 3 - FermentationStationMenu.IN_X - 4;     // the name may run under the arrow, up to the brandy's frame
		if (brew != null) {
			g.text(font, fit(new ItemStack(brew.brandy()).getHoverName().getString(), room), FermentationStationMenu.IN_X, statusY, AMBER);
		} else if (ingredients.stream().anyMatch(stack -> !stack.isEmpty())) {
			g.text(font, fit(Component.translatable("fermentation.endrealms.no_recipe").getString(), room), FermentationStationMenu.IN_X, statusY, BAD);
		} else {
			g.text(font, fit(Component.translatable("fermentation.endrealms.ingredients").getString(), room), FermentationStationMenu.IN_X, statusY, MUTED);
		}
		g.text(font, Component.translatable("fermentation.endrealms.bottle"), FermentationStationMenu.BOTTLE_X + 20, FermentationStationMenu.BOTTLE_Y + 5, MUTED);
		int left = menu.ticksLeft();
		if (left > 0) {
			int seconds = (left + 19) / 20;
			g.text(font, String.format("%d:%02d", seconds / 60, seconds % 60), ARROW_X + 4, ARROW_Y - 12, AMBER);
		}
	}

	private String fit(String text, int width) {
		return font.width(text) <= width ? text : font.plainSubstrByWidth(text, width - font.width("…")) + "…";
	}
}
