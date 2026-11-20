package hu.endrealms.brew;

import hu.endrealms.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The window: three bottle slots side by side, each with a bar to its next quality under it, the player's inventory below. */
public class AgingBarrelMenu extends AbstractContainerMenu {
	public static final int HEIGHT = 176;
	public static final int SLOT_X = 44;
	public static final int SLOT_STEP = 36;
	public static final int SLOT_Y = 26;
	public static final int INVENTORY_Y = HEIGHT - 82;
	private static final int PLAYER_START = AgingBarrelBlockEntity.SIZE;
	private static final int PLAYER_END = PLAYER_START + 36;

	private final Container barrel;
	private final ContainerData data;

	public AgingBarrelMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(AgingBarrelBlockEntity.SIZE), new SimpleContainerData(AgingBarrelBlockEntity.SIZE * 2));
	}

	public AgingBarrelMenu(int id, Inventory inventory, Container barrel, ContainerData data) {
		super(ModMenus.AGING_BARREL, id);
		this.barrel = barrel;
		this.data = data;
		for (int i = 0; i < AgingBarrelBlockEntity.SIZE; i++) {
			addSlot(new Slot(barrel, i, SLOT_X + i * SLOT_STEP, SLOT_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return Brewing.isBrandy(stack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}

				@Override
				public int getMaxStackSize(ItemStack stack) {
					return 1;
				}
			});
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
		}
		addDataSlots(data);
	}

	/** 0..1, how far this slot is to its next quality (0 when empty or already the best). */
	public float progress(int slot) {
		return data.get(slot) / 1000.0F;
	}

	/** Seconds left to the next quality. */
	public int secondsLeft(int slot) {
		return data.get(AgingBarrelBlockEntity.SIZE + slot);
	}

	public ItemStack bottle(int slot) {
		return slots.get(slot).getItem();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		boolean moved;
		if (index < PLAYER_START) {
			moved = moveItemStackTo(stack, PLAYER_START, PLAYER_END, true);
		} else if (Brewing.isBrandy(stack)) {
			moved = false;                     // one bottle into the first empty slot
			for (int i = 0; i < AgingBarrelBlockEntity.SIZE && !moved; i++) {
				Slot target = slots.get(i);
				if (!target.hasItem()) {
					target.setByPlayer(stack.split(1));
					moved = true;
				}
			}
		} else {
			moved = false;
		}
		if (!moved) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return barrel.stillValid(player);
	}
}
