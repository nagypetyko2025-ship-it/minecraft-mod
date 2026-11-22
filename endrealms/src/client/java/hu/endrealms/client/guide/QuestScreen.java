package hu.endrealms.client.guide;

import hu.endrealms.quest.QuestBook;
import hu.endrealms.quest.Quests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Endwalker's Guide: two realms on tabs along the top, the End and the Sculk dimension; the chosen realm's chapters
 * listed down the left side, its Whole Path (every quest in one tree) first; each chapter a tree you can drag around.
 * A quest opens once the quests before it are claimed; its details slide over the right side of the tree. The tree
 * itself comes from QuestBook.
 */
public class QuestScreen extends Screen {
	private static final int CELL_X = 42;
	private static final int CELL_Y = 34;
	private static final int NODE = 24;
	private static final int SLOT = 18;
	private static final int DETAIL_W = 176;
	private static final int BG = 0xF0120D24;
	private static final int CANVAS = 0xFF0F0A1E;
	private static final int BORDER = 0xFF5B3FA0;
	private static final int ACCENT = 0xFF7FE0E8;
	private static final int TEXT = 0xFFE4DEF5;
	private static final int MUTED = 0xFF9A90B8;
	private static final int GOLD = 0xFFF2D27A;
	private static final int LOCKED = 0xFF3A3050;
	private static final int SIDEBAR = 100;
	private static final int ROW = 22;

	private static Set<String> claimed = new HashSet<>();
	private static Map<String, Integer> progress = Map.of();
	private static String lastChapter;
	private static String lastRealm = "end";

	private enum Status { LOCKED, OPEN, READY, CLAIMED }

	private int left;
	private int top;
	private int panelW;
	private int panelH;
	private int chapter;
	private String realm = "end";
	private double panX;
	private double panY;
	private boolean centred;
	private QuestBook.Quest selected;
	private int detailScroll;
	private int detailHeight;
	private Button claimButton;
	private Button closeButton;

	public QuestScreen() {
		super(Component.translatable("quest.endrealms.title"));
	}

	public static void accept(Quests.State state) {
		claimed = new HashSet<>(state.claimed());
		progress = state.progress();
	}

	private static ItemStack stack(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
	}

	private static int amount(QuestBook.Quest quest, int objective) {
		return progress.getOrDefault(quest.id() + "#" + objective, 0);
	}

	private static boolean finished(QuestBook.Quest quest) {
		for (int i = 0; i < quest.objectives().size(); i++) {
			if (amount(quest, i) < quest.objectives().get(i).count()) {
				return false;
			}
		}
		return true;
	}

	private static Status status(QuestBook.Quest quest) {
		if (claimed.contains(quest.id())) {
			return Status.CLAIMED;
		}
		if (!claimed.containsAll(quest.requires())) {
			return Status.LOCKED;
		}
		return finished(quest) ? Status.READY : Status.OPEN;
	}

	private List<QuestBook.Chapter> realmChapters() {
		return QuestBook.chapters(realm);
	}

	private QuestBook.Chapter current() {
		List<QuestBook.Chapter> chapters = realmChapters();
		return chapters.get(Math.min(chapter, chapters.size() - 1));
	}

	private List<QuestBook.Quest> shown() {
		return QuestBook.inChapter(current().id());
	}

	private int canvasLeft() {
		return left + 4 + SIDEBAR + 4;
	}

	private int canvasRight() {
		return left + panelW - 4;
	}

	@Override
	protected void init() {
		panelW = Math.min(width - 12, 540);
		panelH = Math.min(height - 12, 270);
		left = (width - panelW) / 2;
		top = (height - panelH) / 2;
		claimButton = addRenderableWidget(Button.builder(Component.translatable("quest.endrealms.claim"), b -> {
			if (selected != null) {
				ClientPlayNetworking.send(new Quests.Claim(selected.id()));
			}
		}).bounds(left + panelW - DETAIL_W + 8, top + panelH - 26, DETAIL_W - 52, 20).build());
		closeButton = addRenderableWidget(Button.builder(Component.literal("✕"), b -> select(null)).bounds(left + panelW - 28, top + panelH - 26, 22, 20).build());
		realm = lastRealm;
		chapter = 0;
		if (lastChapter != null) {
			for (int i = 0; i < realmChapters().size(); i++) {
				if (realmChapters().get(i).id().equals(lastChapter)) {
					chapter = i;
				}
			}
		}
		centred = false;
		select(selected);
		ClientPlayNetworking.send(new Quests.Request());
	}

	private void select(QuestBook.Quest quest) {
		selected = quest;
		detailScroll = 0;
		claimButton.visible = quest != null;
		closeButton.visible = quest != null;
	}

	private int canvasTop() {
		return top + 48;
	}

	private int canvasBottom() {
		return top + panelH - 4;
	}

	private boolean wholePath() {
		return current().path();
	}

	private void centre() {
		List<QuestBook.Quest> quests = shown();
		if (wholePath()) {
			panX = 14.0;
			panY = (canvasBottom() - canvasTop() - NODE) / 2.0;
			centred = true;
			return;
		}
		float maxX = 0;
		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (QuestBook.Quest quest : quests) {
			maxX = Math.max(maxX, quest.x());
			minY = Math.min(minY, quest.y());
			maxY = Math.max(maxY, quest.y());
		}
		panX = Math.max(14.0, (canvasRight() - canvasLeft() - (maxX * CELL_X + NODE)) / 2.0);
		panY = (canvasBottom() - canvasTop() - NODE) / 2.0 - (minY + maxY) / 2.0 * CELL_Y;
		centred = true;
	}

	private int nodeX(QuestBook.Quest quest) {
		return canvasLeft() + (int) panX + Math.round((wholePath() ? quest.pathX() : quest.x()) * CELL_X);
	}

	private int nodeY(QuestBook.Quest quest) {
		return canvasTop() + (int) panY + Math.round((wholePath() ? quest.pathY() : quest.y()) * CELL_Y);
	}

	private boolean overDetail(double x) {
		return selected != null && x >= left + panelW - DETAIL_W;
	}

	private QuestBook.Quest nodeAt(double x, double y) {
		if (overDetail(x) || x < canvasLeft() || y < canvasTop() || y >= canvasBottom()) {
			return null;
		}
		for (QuestBook.Quest quest : shown()) {
			int nx = nodeX(quest);
			int ny = nodeY(quest);
			if (x >= nx && x < nx + NODE && y >= ny && y < ny + NODE) {
				return quest;
			}
		}
		return null;
	}

	private int realmTabWidth() {
		return 120;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		if (super.mouseClicked(event, doubled)) {
			return true;
		}
		if (event.y() >= top + 20 && event.y() < top + 20 + ROW && event.x() >= left + 4) {     // the realm tabs
			int tab = (int) (event.x() - left - 4) / realmTabWidth();
			if (tab >= 0 && tab < QuestBook.REALMS.size() && !QuestBook.chapters(QuestBook.REALMS.get(tab)).isEmpty()) {
				realm = QuestBook.REALMS.get(tab);
				lastRealm = realm;
				chapter = 0;
				lastChapter = current().id();
				centred = false;
				select(null);
				return true;
			}
		}
		if (event.x() >= left + 4 && event.x() < left + 4 + SIDEBAR && event.y() >= canvasTop()) {   // the chapter list
			int row = (int) (event.y() - canvasTop()) / ROW;
			if (row >= 0 && row < realmChapters().size()) {
				chapter = row;
				lastChapter = current().id();
				centred = false;
				select(null);
				return true;
			}
		}
		QuestBook.Quest quest = nodeAt(event.x(), event.y());
		if (quest != null) {
			select(quest);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (!overDetail(event.x()) && event.x() >= canvasLeft() && event.y() >= canvasTop()) {
			panX += dx;
			panY += dy;
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		if (overDetail(x)) {
			int view = detailBottom() - canvasTop();
			detailScroll = Math.max(0, Math.min(Math.max(0, detailHeight - view), detailScroll - (int) (dy * 14)));
		} else if (wholePath()) {
			panX += dy * 30;
		} else {
			panY += dy * 14;
		}
		return true;
	}

	private int detailBottom() {
		return top + panelH - 30;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, width, height, 0xFF07040F);
		long now = System.currentTimeMillis() / 90;
		for (int i = 0; i < 160; i++) {
			int x = (i * 7919 + i * i * 31) % Math.max(1, width);
			int y = (i * 6271 + i * i * 17) % Math.max(1, height);
			int alpha = 60 + (int) ((now + i * 13) % 40) * 4;
			g.fill(x, y, x + 1, y + 1, alpha << 24 | 0xC8B8FF);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		if (realmChapters().isEmpty()) {
			return;
		}
		if (!centred) {
			centre();
		}
		g.fill(left, top, left + panelW, top + panelH, BG);
		g.outline(left, top, panelW, panelH, BORDER);
		g.text(font, title, left + 8, top + 7, ACCENT);
		List<QuestBook.Quest> inRealm = QuestBook.inRealm(realm);
		Component count = Component.translatable("quest.endrealms.progress", inRealm.stream().filter(q -> claimed.contains(q.id())).count(), inRealm.size());
		g.text(font, count, left + panelW - font.width(count) - 8, top + 7, MUTED);
		extractTabs(g, mouseX, mouseY);
		extractTree(g, mouseX, mouseY);
		if (selected != null) {
			extractDetail(g, mouseX, mouseY);
			claimButton.active = status(selected) == Status.READY;
			claimButton.setMessage(Component.translatable(claimed.contains(selected.id()) ? "quest.endrealms.claimed" : "quest.endrealms.claim"));
		}
		super.extractRenderState(g, mouseX, mouseY, partial);
	}

	private void extractTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		// the two realms along the top
		for (int i = 0; i < QuestBook.REALMS.size(); i++) {
			String id = QuestBook.REALMS.get(i);
			List<QuestBook.Chapter> chapters = QuestBook.chapters(id);
			if (chapters.isEmpty()) {
				continue;
			}
			int x = left + 4 + i * realmTabWidth();
			int y = top + 20;
			boolean active = id.equals(realm);
			g.fill(x, y, x + realmTabWidth() - 3, y + ROW, active ? 0xFF33275C : 0xFF1A1433);
			g.outline(x, y, realmTabWidth() - 3, ROW, active ? ACCENT : BORDER);
			g.item(stack(chapters.get(0).icon()), x + 3, y + 3);
			g.text(font, Component.translatable("quest.endrealms.realm." + id), x + 22, y + 7, active ? TEXT : MUTED);
			if (QuestBook.inRealm(id).stream().anyMatch(q -> status(q) == Status.READY)) {
				g.fill(x + realmTabWidth() - 10, y + 2, x + realmTabWidth() - 6, y + 6, GOLD);
			}
		}
		// the chosen realm's chapters down the left
		List<QuestBook.Chapter> chapters = realmChapters();
		int x = left + 4;
		g.fill(x, canvasTop(), x + SIDEBAR, canvasBottom(), 0xFF140F28);
		for (int i = 0; i < chapters.size(); i++) {
			int y = canvasTop() + i * ROW;
			if (y + ROW > canvasBottom()) {
				break;
			}
			boolean active = i == chapter;
			if (active) {
				g.fill(x, y, x + SIDEBAR, y + ROW - 1, 0xFF33275C);
				g.fill(x, y, x + 2, y + ROW - 1, ACCENT);
			}
			g.item(stack(chapters.get(i).icon()), x + 4, y + 3);
			Component name = Component.translatable("quest.endrealms.chapter." + chapters.get(i).id());
			String label = name.getString();
			int room = SIDEBAR - 30;
			String shownLabel = font.width(label) > room ? font.plainSubstrByWidth(label, room - 5) + "…" : label;
			g.text(font, shownLabel, x + 23, y + 7, active ? TEXT : MUTED);
			boolean ready = QuestBook.inChapter(chapters.get(i).id()).stream().anyMatch(q -> status(q) == Status.READY);
			if (ready) {
				g.fill(x + SIDEBAR - 6, y + 3, x + SIDEBAR - 2, y + 7, GOLD);
			}
			if (mouseX >= x && mouseX < x + SIDEBAR && mouseY >= y && mouseY < y + ROW && font.width(label) > room) {
				g.setTooltipForNextFrame(font, name, mouseX, mouseY);
			}
		}
	}

	private void extractTree(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x0 = canvasLeft();
		int x1 = canvasRight();
		g.fill(x0, canvasTop(), x1, canvasBottom(), CANVAS);
		g.enableScissor(x0, canvasTop(), x1, canvasBottom());
		List<QuestBook.Quest> quests = shown();
		boolean path = wholePath();
		if (path) {
			extractTierLines(g);
		}
		for (QuestBook.Quest quest : quests) {
			for (String required : quest.requires()) {
				QuestBook.Quest parent = QuestBook.quest(required);
				if (parent != null && (path || parent.chapter().equals(quest.chapter()))) {
					connect(g, parent, quest, 1);
				}
			}
			if (path && quest.suggestedAfter() != null) {
				connect(g, QuestBook.quest(quest.suggestedAfter()), quest, 0);
			}
		}
		QuestBook.Quest hovered = null;
		for (QuestBook.Quest quest : quests) {
			Status status = status(quest);
			int x = nodeX(quest);
			int y = nodeY(quest);
			int frame = switch (status) {
				case LOCKED -> LOCKED;
				case OPEN -> BORDER;
				case READY -> (System.currentTimeMillis() / 400 % 2 == 0) ? GOLD : 0xFFB89A50;
				case CLAIMED -> ACCENT;
			};
			g.fill(x, y, x + NODE, y + NODE, status == Status.LOCKED ? 0xFF171126 : 0xFF261D47);
			g.outline(x, y, NODE, NODE, frame);
			if (quest == selected) {
				g.outline(x - 2, y - 2, NODE + 4, NODE + 4, TEXT);
			}
			g.item(stack(quest.icon()), x + 4, y + 4);
			if (status == Status.LOCKED) {
				g.fill(x + 1, y + 1, x + NODE - 1, y + NODE - 1, 0x99171126);
			}
			if (status == Status.CLAIMED) {
				g.text(font, "✔", x + NODE - 8, y + NODE - 9, ACCENT);
			}
			if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE && mouseY >= canvasTop() && mouseY < canvasBottom() && !overDetail(mouseX)) {
				hovered = quest;
			}
		}
		if (path) {
			extractTierNames(g);
		}
		g.disableScissor();
		if (hovered != null) {
			g.setTooltipForNextFrame(font, title(hovered), mouseX, mouseY);
		}
	}

	private int tierX(QuestBook.Tier tier) {
		return canvasLeft() + (int) panX + Math.round(tier.x() * CELL_X);
	}

	/** Only a line separates the tiers: the quests stay joined across it. */
	private void extractTierLines(GuiGraphicsExtractor g) {
		List<QuestBook.Tier> tiers = QuestBook.tiers(realm);
		for (QuestBook.Tier tier : tiers) {
			if (tier != tiers.get(0)) {
				int x = tierX(tier) - (CELL_X - NODE) / 2;
				g.fill(x, canvasTop(), x + 1, canvasBottom(), BORDER);
			}
		}
	}

	private void extractTierNames(GuiGraphicsExtractor g) {
		List<QuestBook.Quest> path = QuestBook.inChapter(current().id());
		for (QuestBook.Tier tier : QuestBook.tiers(realm)) {
			long total = path.stream().filter(q -> q.tier() == tier.id()).count();
			long done = path.stream().filter(q -> q.tier() == tier.id() && claimed.contains(q.id())).count();
			String label = Component.translatable("quest.endrealms.tier." + tier.id()).getString() + " (" + done + "/" + total + ")";
			int x = tierX(tier);
			g.fill(x - 2, canvasTop() + 1, x + font.width(label) + 2, canvasTop() + 12, 0xCC0F0A1E);
			g.text(font, label, x, canvasTop() + 3, GOLD);
		}
	}

	/** thickness 1 is a real requirement; thickness 0 only suggests the order and locks nothing. */
	private void connect(GuiGraphicsExtractor g, QuestBook.Quest from, QuestBook.Quest to, int thickness) {
		int color = claimed.contains(from.id()) ? ACCENT : thickness == 0 ? 0xFF5A4C80 : LOCKED;
		int fx = nodeX(from) + NODE;
		int fy = nodeY(from) + NODE / 2;
		int tx = nodeX(to);
		int ty = nodeY(to) + NODE / 2;
		int mid = tx - (CELL_X - NODE) / 2 + 3;
		int low = thickness == 0 ? 0 : 1;
		g.fill(fx, fy - low, mid + 1, fy + 1, color);
		g.fill(mid - low, Math.min(fy, ty) - low, mid + 1, Math.max(fy, ty) + 1, color);
		g.fill(mid, ty - low, tx, ty + 1, color);
	}

	private static Component title(QuestBook.Quest quest) {
		return Component.translatable("quest.endrealms." + quest.id() + ".title");
	}

	private int wrap(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) {
		for (FormattedCharSequence row : font.split(text, width)) {
			g.text(font, row, x, y, color);
			y += 10;
		}
		return y;
	}

	private Component objectiveText(QuestBook.Quest quest, int index) {
		QuestBook.Objective objective = quest.objectives().get(index);
		Identifier id = Identifier.parse(objective.id());
		Component name = switch (objective.type()) {
			case "item" -> Component.translatable("quest.endrealms.obtain", new ItemStack(BuiltInRegistries.ITEM.getValue(id)).getHoverName());
			case "kill" -> Component.translatable("quest.endrealms.kill", BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription());
			case "mine" -> Component.translatable("quest.endrealms.mine", BuiltInRegistries.BLOCK.getValue(id).getName());
			default -> Component.translatable("quest.endrealms." + quest.id() + ".goal");
		};
		if (objective.count() <= 1) {
			return name;
		}
		return Component.empty().append(name).append(" (" + Math.min(amount(quest, index), objective.count()) + "/" + objective.count() + ")");
	}

	private void extractDetail(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int px = left + panelW - DETAIL_W;
		g.fill(px, canvasTop(), left + panelW - 4, canvasBottom(), 0xFF1A1433);
		g.fill(px, canvasTop(), px + 1, canvasBottom(), BORDER);
		int bottom = detailBottom();
		int x = px + 8;
		int w = DETAIL_W - 18;
		g.enableScissor(px + 1, canvasTop(), left + panelW - 4, bottom);
		int start = canvasTop() + 5 - detailScroll;
		int y = wrap(g, title(selected), x, start, w, ACCENT) + 2;
		Status status = status(selected);
		y = wrap(g, Component.translatable("quest.endrealms.status." + status.name().toLowerCase(java.util.Locale.ROOT)), x, y, w, status == Status.READY ? GOLD : MUTED) + 4;
		y = wrap(g, Component.translatable("quest.endrealms." + selected.id() + ".desc"), x, y, w, TEXT) + 5;
		if (status == Status.LOCKED) {
			g.text(font, Component.translatable("quest.endrealms.requires"), x, y, MUTED);
			y += 10;
			for (String required : selected.requires()) {
				if (!claimed.contains(required) && QuestBook.quest(required) != null) {
					y = wrap(g, Component.literal("• ").append(title(QuestBook.quest(required))), x, y, w, GOLD);
				}
			}
			y += 4;
		}
		g.text(font, Component.translatable("quest.endrealms.goals"), x, y, MUTED);
		y += 10;
		for (int i = 0; i < selected.objectives().size(); i++) {
			boolean done = amount(selected, i) >= selected.objectives().get(i).count();
			y = wrap(g, Component.literal(done ? "✔ " : "• ").append(objectiveText(selected, i)), x, y, w, done ? ACCENT : TEXT);
		}
		y += 5;
		boolean canHover = mouseY >= canvasTop() && mouseY < bottom;
		if (!selected.rewards().isEmpty() || selected.xp() > 0) {
			g.text(font, Component.translatable("quest.endrealms.rewards"), x, y, MUTED);
			y += 11;
			int rx = x;
			for (QuestBook.Reward reward : selected.rewards()) {
				if (rx + SLOT > x + w) {
					rx = x;
					y += SLOT;
				}
				ItemStack shown = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(reward.item())), reward.count());
				slot(g, shown, rx, y, mouseX, mouseY, canHover);
				rx += SLOT;
			}
			y += selected.rewards().isEmpty() ? 0 : SLOT + 2;
			if (selected.xp() > 0) {
				g.text(font, Component.translatable("quest.endrealms.xp", selected.xp()), x, y, 0xFFB0F070);
				y += 11;
			}
			y += 3;
		}
		for (QuestBook.RecipeView recipe : selected.recipes()) {
			y = extractRecipe(g, recipe, x, y, mouseX, mouseY, canHover) + 6;
		}
		detailHeight = y - start + 4;
		g.disableScissor();
	}

	private void slot(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int mouseX, int mouseY, boolean canHover) {
		g.fill(x, y, x + SLOT - 1, y + SLOT - 1, 0xFF2A2147);
		g.outline(x, y, SLOT - 1, SLOT - 1, 0xFF4A3A78);
		if (!stack.isEmpty()) {
			g.item(stack, x, y);
			g.itemDecorations(font, stack, x, y);
			if (canHover && mouseX >= x && mouseX < x + SLOT - 1 && mouseY >= y && mouseY < y + SLOT - 1) {
				g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
			}
		}
	}

	private static ItemStack cycle(List<String> options) {
		return options.isEmpty() ? ItemStack.EMPTY : stack(options.get((int) (System.currentTimeMillis() / 1000 % options.size())));
	}

	private int extractRecipe(GuiGraphicsExtractor g, QuestBook.RecipeView recipe, int x, int y, int mouseX, int mouseY, boolean canHover) {
		int side = "crafting".equals(recipe.kind()) ? 3 : "end_crafting".equals(recipe.kind()) ? 5 : 0;     // grids: 3x3 or the End Crafting Table's 5x5
		boolean crafting = side > 0;
		int inputs = crafting ? side : recipe.slots().size();
		int height = crafting ? side * SLOT : SLOT;
		for (int i = 0; i < recipe.slots().size(); i++) {
			int sx = crafting ? x + i % side * SLOT : x + i * SLOT;
			int sy = crafting ? y + i / side * SLOT : y;
			slot(g, cycle(recipe.slots().get(i)), sx, sy, mouseX, mouseY, canHover);
		}
		int middle = y + height / 2 - SLOT / 2;
		g.text(font, "→", x + inputs * SLOT + 6, middle + 5, MUTED);
		slot(g, stack(recipe.result()), x + inputs * SLOT + 22, middle, mouseX, mouseY, canHover);
		return y + height;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
