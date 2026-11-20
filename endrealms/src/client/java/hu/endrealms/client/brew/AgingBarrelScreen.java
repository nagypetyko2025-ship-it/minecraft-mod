package hu.endrealms.client.brew;

import hu.endrealms.brew.AgingBarrelBlockEntity;
import hu.endrealms.brew.AgingBarrelMenu;
import hu.endrealms.brew.Brewing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Aging Barrel's window: three bottles, each with its quality in stars, a bar to the next star and the time left. */
public class AgingBarrelScreen extends AbstractContainerScreen<AgingBarrelMenu> {
	private static final int PANEL = 0xFF231910;
	private static final int BORDER = 0xFF7A6A5A;
	private static final int SLOT_BG = 0xFF3B2A1C;
	private static final int SLOT_EDGE = 0xFF6E4A30;
	private static final int TEXT = 0xFFF3E6D2;
	private static final int MUTED = 0xFFB8A48C;
	private static final int AMBER = 0xFFE8A33A;
	private static final int GOLD = 0xFFFFD45C;
	private static final int DIM = 0xFF5A4A3A;
	private static final int BAR_W = 30;
	private static final int STARS_Y = AgingBarrelMenu.SLOT_Y + 21;
	private static final int BAR_Y = AgingBarrelMenu.SLOT_Y + 32;
	private static final int TIME_Y = AgingBarrelMenu.SLOT_Y + 38;

	public AgingBarrelScreen(AgingBarrelMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, AgingBarrelMenu.HEIGHT);
		this.titleLabelY = 6;
		this.inventoryLabelY = AgingBarrelMenu.INVENTORY_Y - 11;
	}

	private int centre(int slot) {
		return AgingBarrelMenu.SLOT_X + slot * AgingBarrelMenu.SLOT_STEP + 8;
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
		for (int i = 0; i < AgingBarrelBlockEntity.SIZE; i++) {
			int x = leftPos + centre(i) - BAR_W / 2;
			int y = topPos + BAR_Y;
			g.fill(x, y, x + BAR_W, y + 4, SLOT_BG);
			g.outline(x - 1, y - 1, BAR_W + 2, 6, SLOT_EDGE);
			ItemStack bottle = menu.bottle(i);
			if (Brewing.isBrandy(bottle) && Brewing.quality(bottle) >= Brewing.MAX_QUALITY) {
				g.fill(x, y, x + BAR_W, y + 4, GOLD);
			} else {
				g.fill(x, y, x + Math.round(BAR_W * menu.progress(i)), y + 4, AMBER);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, titleLabelX, titleLabelY, AMBER);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT);
		for (int i = 0; i < AgingBarrelBlockEntity.SIZE; i++) {
			ItemStack bottle = menu.bottle(i);
			int cx = centre(i);
			if (!Brewing.isBrandy(bottle)) {
				continue;
			}
			int quality = Brewing.quality(bottle);
			int starsWidth = font.width("★★★");
			int sx = cx - starsWidth / 2;
			for (int star = 0; star < Brewing.MAX_QUALITY; star++) {
				g.text(font, "★", sx, STARS_Y, star < quality ? GOLD : DIM);
				sx += font.width("★");
			}
			String time;
			int color;
			if (quality >= Brewing.MAX_QUALITY) {
				time = Component.translatable("aging.endrealms.done").getString();
				color = GOLD;
			} else {
				int seconds = menu.secondsLeft(i);
				time = String.format("%d:%02d", seconds / 60, seconds % 60);
				color = MUTED;
			}
			g.text(font, time, cx - font.width(time) / 2, TIME_Y, color);
		}
	}
}
