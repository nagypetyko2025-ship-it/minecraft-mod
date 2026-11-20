package hu.endrealms.brew;

import hu.endrealms.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The window: three ingredient slots and the bottle on the left (the water tank beside them), the brandy on the right with
 * the Start / Stop button under the arrow, the player's inventory below.
 */
public class FermentationStationMenu extends AbstractContainerMenu {
	public static final int HEIGHT = 196;
	public static final int IN_X = 30;
	public static final int IN_STEP = 22;
	public static final int IN_Y = 24;
	public static final int BOTTLE_X = 30;
	public static final int BOTTLE_Y = 70;
	public static final int OUT_X = 150;
	public static final int OUT_Y = 40;
	public static final int INVENTORY_Y = HEIGHT - 82;
	public static final int BUTTON_START = 0;
	private static final int PLAYER_START = FermentationStationBlockEntity.SIZE;
	private static final int PLAYER_END = PLAYER_START + 36;

	private final Container station;
	private final ContainerData data;

	/** Client side: contents and progress arrive through the usual sync. */
	public FermentationStationMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(FermentationStationBlockEntity.SIZE), new SimpleContainerData(4));
	}

	public FermentationStationMenu(int id, Inventory inventory, Container station, ContainerData data) {
		super(ModMenus.FERMENTATION_STATION, id);
		this.station = station;
		this.data = data;
		for (int i = 0; i < Brewing.SLOTS; i++) {
			addSlot(new Slot(station, FermentationStationBlockEntity.INGREDIENTS + i, IN_X + i * IN_STEP, IN_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return Brewing.isIngredient(stack);
				}
			});
		}
		addSlot(new Slot(station, FermentationStationBlockEntity.BOTTLES, BOTTLE_X, BOTTLE_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(Items.GLASS_BOTTLE);
			}
		});
		addSlot(new Slot(station, FermentationStationBlockEntity.OUTPUT, OUT_X, OUT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
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

	/** 0..1, how far the current bottle is. */
	public float progress() {
		int total = data.get(1);
		return total <= 0 ? 0.0F : Math.min(1.0F, data.get(0) / (float) total);
	}

	public int water() {
		return data.get(2);
	}

	public boolean running() {
		return data.get(3) != 0;
	}

	public int ticksLeft() {
		return running() ? data.get(1) - data.get(0) : 0;
	}

	public List<ItemStack> ingredients() {
		List<ItemStack> list = new ArrayList<>();
		for (int i = 0; i < Brewing.SLOTS; i++) {
			list.add(slots.get(i).getItem());
		}
		return list;
	}

	/** Whether Start would do something: a recipe, a bottle, water and room for the brandy. */
	public boolean canStart() {
		Brewing.Match match = Brewing.match(ingredients());
		ItemStack out = slots.get(FermentationStationBlockEntity.OUTPUT).getItem();
		return match != null && water() > 0 && slots.get(FermentationStationBlockEntity.BOTTLES).getItem().is(Items.GLASS_BOTTLE)
				&& (out.isEmpty() || out.is(match.brew().brandy()) && out.getCount() < out.getMaxStackSize());
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == BUTTON_START && station instanceof FermentationStationBlockEntity be) {
			be.toggle();
			return true;
		}
		return false;
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
		} else if (Brewing.isIngredient(stack)) {
			moved = false;                     // into the slot that already holds this kind, else the first empty one
			for (int i = 0; i < Brewing.SLOTS && !moved; i++) {
				if (slots.get(i).getItem().is(stack.getItem())) {
					moved = moveItemStackTo(stack, i, i + 1, false);
				}
			}
			for (int i = 0; i < Brewing.SLOTS && !moved; i++) {
				if (!slots.get(i).hasItem()) {
					moved = moveItemStackTo(stack, i, i + 1, false);
				}
			}
		} else if (stack.is(Items.GLASS_BOTTLE)) {
			moved = moveItemStackTo(stack, FermentationStationBlockEntity.BOTTLES, FermentationStationBlockEntity.BOTTLES + 1, false);
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
		return station.stillValid(player);
	}
}
