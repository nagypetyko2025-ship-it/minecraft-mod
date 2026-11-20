package hu.endrealms.advancement;

import hu.endrealms.EndRealms;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementNode;

/**
 * Lays out the End Realms tab around its root: exploration and creatures branch off to the left, materials, gear and
 * the boss to the right. The game itself only knows how to grow a tree rightwards, and its scrolling cannot reach
 * negative coordinates, so the finished tree is moved until its top left corner is at (0, 0).
 */
public final class AdvancementLayout {
	private static final Set<String> LEFT = Set.of("all_biomes", "end_village", "void_bastion", "shardcrab", "astral_fish", "starberry", "sky_wreck", "observatory", "crypt");

	private AdvancementLayout() {
	}

	public static void apply(AdvancementNode root) {
		if (!root.holder().id().equals(EndRealms.id("root"))) {
			return;
		}
		List<AdvancementNode> left = new ArrayList<>();
		List<AdvancementNode> right = new ArrayList<>();
		for (AdvancementNode child : sorted(root)) {
			(LEFT.contains(child.holder().id().getPath()) ? left : right).add(child);
		}
		Map<AdvancementNode, float[]> positions = new IdentityHashMap<>();
		positions.put(root, new float[] {0.0F, 0.0F});
		placeSide(left, -1, positions);
		placeSide(right, 1, positions);
		float minX = 0.0F;
		float minY = 0.0F;
		for (float[] position : positions.values()) {
			minX = Math.min(minX, position[0]);
			minY = Math.min(minY, position[1]);
		}
		for (Map.Entry<AdvancementNode, float[]> entry : positions.entrySet()) {
			entry.getKey().setLocation(entry.getValue()[0] - minX, entry.getValue()[1] - minY);
		}
	}

	private static List<AdvancementNode> sorted(AdvancementNode node) {
		List<AdvancementNode> children = new ArrayList<>();
		node.children().forEach(children::add);
		children.sort(Comparator.comparing(child -> child.holder().id().getPath()));
		return children;
	}

	private static int leaves(AdvancementNode node) {
		int count = 0;
		for (AdvancementNode child : node.children()) {
			count += leaves(child);
		}
		return Math.max(1, count);
	}

	/** Each side is centred on the root's row. */
	private static void placeSide(List<AdvancementNode> side, int direction, Map<AdvancementNode, float[]> positions) {
		int rows = 0;
		for (AdvancementNode node : side) {
			rows += leaves(node);
		}
		float[] row = {-(rows - 1) / 2.0F};
		for (AdvancementNode node : side) {
			place(node, 1, direction, row, positions);
		}
	}

	private static float place(AdvancementNode node, int depth, int direction, float[] row, Map<AdvancementNode, float[]> positions) {
		List<AdvancementNode> children = sorted(node);
		float y;
		if (children.isEmpty()) {
			y = row[0]++;
		} else {
			float first = 0.0F;
			float last = 0.0F;
			for (int i = 0; i < children.size(); i++) {
				float childY = place(children.get(i), depth + 1, direction, row, positions);
				if (i == 0) {
					first = childY;
				}
				last = childY;
			}
			y = (first + last) / 2.0F;
		}
		positions.put(node, new float[] {direction * depth, y});
		return y;
	}
}
