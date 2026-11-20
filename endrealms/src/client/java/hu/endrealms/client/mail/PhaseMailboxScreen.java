package hu.endrealms.client.mail;

import hu.endrealms.mail.PhaseMail;
import hu.endrealms.mail.PhaseMailboxMenu;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** The mail window: inbox on the left; on the right this mailbox's name, the list of every mailbox, and the send slot. */
public class PhaseMailboxScreen extends AbstractContainerScreen<PhaseMailboxMenu> {
	private static final int PANEL = 0xFF17102E;
	private static final int BORDER = 0xFF5B3FA0;
	private static final int SLOT_BG = 0xFF2A2147;
	private static final int SLOT_EDGE = 0xFF4A3A78;
	private static final int TEXT = 0xFFE4DEF5;
	private static final int MUTED = 0xFF9A90B8;
	private static final int ACCENT = 0xFF7FE0E8;
	private static final int GOLD = 0xFFF2D27A;
	private static final int SIDE_X = 180;
	private static final int SIDE_W = 124;
	private static final int LIST_Y = 62;
	private static final int ROW = 11;
	private static final int ROWS = 5;

	private static PhaseMail.State state;

	private EditBox nameBox;
	private Button saveButton;
	private Button sendButton;
	private String selected;
	private int scroll;

	public PhaseMailboxScreen(PhaseMailboxMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, SIDE_X + SIDE_W + 4, 166);
		this.titleLabelY = 6;
		state = null;
	}

	/** Called by the client network handler; refreshes an open mail window. */
	public static void accept(PhaseMail.State newState) {
		state = newState;
		if (net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof PhaseMailboxScreen screen) {
			screen.refresh();
		}
	}

	@Override
	protected void init() {
		super.init();
		nameBox = addRenderableWidget(new EditBox(font, leftPos + SIDE_X, topPos + 18, SIDE_W - 34, 14, Component.translatable("mail.endrealms.name")));
		nameBox.setMaxLength(24);
		saveButton = addRenderableWidget(Button.builder(Component.literal("✔"), b -> {
			if (state != null) {
				ClientPlayNetworking.send(new PhaseMail.Rename(state.pos(), nameBox.getValue()));
			}
		}).bounds(leftPos + SIDE_X + SIDE_W - 30, topPos + 17, 30, 16).build());
		sendButton = addRenderableWidget(Button.builder(Component.translatable("mail.endrealms.send"), b -> {
			if (state != null && selected != null) {
				ClientPlayNetworking.send(new PhaseMail.Send(state.pos(), selected));
			}
		}).bounds(leftPos + PhaseMailboxMenu.SEND_X + 22, topPos + PhaseMailboxMenu.SEND_Y - 2, SIDE_W - 26, 20).build());
		refresh();
	}

	private void refresh() {
		if (nameBox == null) {
			return;
		}
		boolean ready = state != null;
		if (ready && !nameBox.isFocused()) {
			nameBox.setValue(state.name());
		}
		nameBox.setEditable(ready && state.owner());
		saveButton.active = ready && state.owner();
		if (ready && selected != null && !state.names().contains(selected)) {
			selected = null;
		}
		sendButton.active = ready && selected != null;
	}

	private List<String> names() {
		return state == null ? List.of() : state.names();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractBackground(g, mouseX, mouseY, partial);
		g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
		g.outline(leftPos, topPos, imageWidth, imageHeight, BORDER);
		g.fill(leftPos + SIDE_X - 4, topPos + 4, leftPos + SIDE_X - 3, topPos + imageHeight - 4, BORDER);
		for (Slot slot : menu.slots) {
			int x = leftPos + slot.x - 1;
			int y = topPos + slot.y - 1;
			g.fill(x, y, x + 18, y + 18, SLOT_BG);
			g.outline(x, y, 18, 18, slot.index == PhaseMailboxMenu.SEND_SLOT ? ACCENT : SLOT_EDGE);
		}
		int listX = leftPos + SIDE_X;
		int listY = topPos + LIST_Y;
		g.fill(listX, listY, listX + SIDE_W, listY + ROW * ROWS + 2, 0xFF0F0A1E);
		g.outline(listX, listY, SIDE_W, ROW * ROWS + 2, SLOT_EDGE);
		List<String> names = names();
		for (int i = 0; i < ROWS && scroll + i < names.size(); i++) {
			String name = names.get(scroll + i);
			int y = listY + 1 + i * ROW;
			boolean own = state != null && name.equals(state.name());
			if (name.equals(selected)) {
				g.fill(listX + 1, y, listX + SIDE_W - 1, y + ROW, 0xFF33275C);
			}
			String owner = state.owners().get(scroll + i);
			String label = font.plainSubstrByWidth(name, SIDE_W - 6 - font.width(owner) - 6);
			g.text(font, label, listX + 3, y + 2, own ? MUTED : name.equals(selected) ? GOLD : TEXT);
			g.text(font, owner, listX + SIDE_W - 3 - font.width(owner), y + 2, MUTED);
		}
		if (names.size() > ROWS) {
			int bar = Math.max(6, (ROW * ROWS) * ROWS / names.size());
			int top = listY + 1 + (ROW * ROWS - bar) * scroll / Math.max(1, names.size() - ROWS);
			g.fill(listX + SIDE_W - 2, top, listX + SIDE_W - 1, top + bar, BORDER);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, titleLabelX, titleLabelY, ACCENT);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT);
		g.text(font, Component.translatable("mail.endrealms.name"), SIDE_X, 7, MUTED);
		g.text(font, Component.translatable("mail.endrealms.recipients", names().size()), SIDE_X, LIST_Y - 10, MUTED);
		if (state != null && !state.message().isEmpty()) {
			int y = 152;
			for (FormattedCharSequence row : font.split(Component.translatable(state.message()), SIDE_W)) {
				g.text(font, row, SIDE_X, y, state.message().endsWith("sent") || state.message().endsWith("renamed") ? ACCENT : GOLD);
				y += 9;
				break;
			}
		}
		if (selected == null) {
			g.text(font, Component.translatable("mail.endrealms.pick"), SIDE_X, LIST_Y + ROW * ROWS + 6, MUTED);
		} else {
			g.text(font, font.plainSubstrByWidth("→ " + selected, SIDE_W), SIDE_X, LIST_Y + ROW * ROWS + 6, GOLD);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		int listX = leftPos + SIDE_X;
		int listY = topPos + LIST_Y + 1;
		if (event.x() >= listX && event.x() < listX + SIDE_W && event.y() >= listY && event.y() < listY + ROW * ROWS) {
			int index = scroll + (int) (event.y() - listY) / ROW;
			if (index < names().size()) {
				selected = names().get(index);
				refresh();
				return true;
			}
		}
		return super.mouseClicked(event, doubled);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		if (x >= leftPos + SIDE_X && x < leftPos + SIDE_X + SIDE_W && y >= topPos + LIST_Y && y < topPos + LIST_Y + ROW * ROWS) {
			scroll = Math.max(0, Math.min(Math.max(0, names().size() - ROWS), scroll - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
	}

	/** While typing the name, letters must not close the window or trigger hotbar keys. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (nameBox != null && nameBox.isFocused() && event.key() != 256) {
			if (event.key() == 257 || event.key() == 335) {
				saveButton.onPress(null);
				nameBox.setFocused(false);
				return true;
			}
			return nameBox.keyPressed(event) || true;
		}
		return super.keyPressed(event);
	}

	public BlockPos pos() {
		return state == null ? BlockPos.ZERO : state.pos();
	}
}
