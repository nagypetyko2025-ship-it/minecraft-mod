package hu.endrealms.bag;

import hu.endrealms.registry.ModItems;
import java.util.function.BooleanSupplier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Backpack: a slot above the off hand holds it, and while it is worn nine more slots open below the inventory. The
 * slots are appended to the player's inventory menu, so the game syncs and clicks them like any other slot.
 */
public final class Bags {
	/** Container layout: the worn bag first, then its nine slots. */
	public static final int WORN = 0;
	public static final int ROWS = 9;
	public static final int SIZE = 1 + ROWS;
	/** Index of the worn bag's slot in the inventory menu, right after the off hand. */
	public static final int MENU_SLOT = 46;
	public static final int WORN_X = 77;
	public static final int WORN_Y = 44;
	public static final int ROW_X = 8;
	public static final int ROW_Y = 171;

	/** Set by the client: the creative inventory lays out menu slots on its own and has no room for these. */
	public static BooleanSupplier hidden = () -> false;

	private Bags() {
	}

	public interface Holder {
		SimpleContainer endrealms$bag();
	}

	public static SimpleContainer of(Player player) {
		return ((Holder) player).endrealms$bag();
	}

	public static boolean worn(Container bag) {
		return !bag.getItem(WORN).isEmpty();
	}

	private static boolean hasContents(Container bag) {
		for (int i = 1; i < SIZE; i++) {
			if (!bag.getItem(i).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Holds the bag itself; a bag with something in it stays on, so nothing inside can get lost. */
	public static final class WornSlot extends Slot {
		public WornSlot(Container bag) {
			super(bag, WORN, WORN_X, WORN_Y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.is(ModItems.BACKPACK);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public boolean mayPickup(Player player) {
			return !hasContents(container);
		}

		@Override
		public boolean isActive() {
			return !hidden.getAsBoolean();
		}
	}

	public static final class StorageSlot extends Slot {
		public StorageSlot(Container bag, int row) {
			super(bag, 1 + row, ROW_X + row * 18, ROW_Y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return worn(container) && !stack.is(ModItems.BACKPACK);
		}

		@Override
		public boolean isActive() {
			return worn(container) && !hidden.getAsBoolean();
		}
	}
}
