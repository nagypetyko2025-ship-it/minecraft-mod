package hu.endrealms.craft;

import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The End Crafting Table: a 5x5 grid, the result, and the player's inventory. Button n fills the grid with recipe n. */
public class EndCraftingMenu extends AbstractContainerMenu {
	public static final int SIZE = EndCraftingRecipe.SIZE;
	public static final int GRID_X = 8;
	public static final int GRID_Y = 18;
	public static final int RESULT_X = 134;
	public static final int RESULT_Y = GRID_Y + 2 * 18;
	public static final int INVENTORY_Y = 124;
	private static final int RESULT = 0;
	private static final int GRID_END = 1 + SIZE * SIZE;
	private static final int INVENTORY_END = GRID_END + 36;

	private final TransientCraftingContainer grid = new TransientCraftingContainer(this, SIZE, SIZE);
	private final ResultContainer result = new ResultContainer();
	private final ContainerLevelAccess access;
	private final Player player;

	public EndCraftingMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public EndCraftingMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(ModMenus.END_CRAFTING_TABLE, id);
		this.access = access;
		this.player = inventory.player;
		addSlot(new ResultSlot(result, RESULT_X, RESULT_Y));
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				addSlot(new Slot(grid, y * SIZE + x, GRID_X + x * 18, GRID_Y + y * 18));
			}
		}
		addStandardInventorySlots(inventory, 8, INVENTORY_Y);
	}

	private Optional<RecipeHolder<EndCraftingRecipe>> recipe(ServerLevel level) {
		return level.recipeAccess().getRecipeFor(EndCrafting.TYPE, CraftingInput.of(SIZE, SIZE, grid.getItems()), level);
	}

	@Override
	public void slotsChanged(Container container) {
		if (container == grid && player instanceof ServerPlayer serverPlayer) {
			ServerLevel level = serverPlayer.level();
			ItemStack made = recipe(level).map(holder -> holder.value().assemble(CraftingInput.of(SIZE, SIZE, grid.getItems()))).orElse(ItemStack.EMPTY);
			result.setItem(0, made);
			setRemoteSlot(RESULT, made);
			serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT, made));
		}
	}

	/** Takes one of everything in the grid; buckets and the like stay behind as their empty selves. */
	private void consume(Player crafter) {
		CraftingInput.Positioned positioned = CraftingInput.ofPositioned(SIZE, SIZE, grid.getItems());
		CraftingInput input = positioned.input();
		NonNullList<ItemStack> leftovers = CraftingRecipe.defaultCraftingReminder(input);
		for (int i = 0; i < input.size(); i++) {
			int slot = (positioned.top() + i / input.width()) * SIZE + positioned.left() + i % input.width();
			if (!grid.getItem(slot).isEmpty()) {
				grid.removeItem(slot, 1);
			}
			ItemStack leftover = leftovers.get(i);
			if (!leftover.isEmpty()) {
				if (grid.getItem(slot).isEmpty()) {
					grid.setItem(slot, leftover);
				} else {
					crafter.getInventory().placeItemBackInInventory(leftover, net.minecraft.util.Prediction.SERVER_ONLY);
				}
			}
		}
		slotsChanged(grid);
	}

	private final class ResultSlot extends Slot {
		ResultSlot(Container container, int x, int y) {
			super(container, 0, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public void onTake(Player crafter, ItemStack stack) {
			stack.onCraftedBy(crafter, stack.getCount());
			consume(crafter);
		}
	}

	/** Button n: put recipe n's ingredients into the grid from the player's inventory, if they have all of them. */
	@Override
	public boolean clickMenuButton(Player clicker, int button) {
		if (!(clicker instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		List<RecipeHolder<EndCraftingRecipe>> recipes = EndCrafting.recipes(serverPlayer.level().getServer());
		if (button < 0 || button >= recipes.size()) {
			return false;
		}
		clearContainer(clicker, grid);       // whatever was in the grid goes back first
		List<Optional<Ingredient>> cells = recipes.get(button).value().grid();
		Inventory inventory = clicker.getInventory();
		List<ItemStack> pool = new ArrayList<>();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			pool.add(inventory.getItem(i).copy());
		}
		int[] from = new int[cells.size()];
		for (int c = 0; c < cells.size(); c++) {
			from[c] = -1;
			if (cells.get(c).isEmpty()) {
				continue;
			}
			for (int i = 0; i < pool.size(); i++) {
				if (!pool.get(i).isEmpty() && cells.get(c).get().test(pool.get(i))) {
					pool.get(i).shrink(1);
					from[c] = i;
					break;
				}
			}
			if (from[c] < 0) {
				return true;          // something is missing: the window still shows the pattern
			}
		}
		for (int c = 0; c < cells.size(); c++) {
			if (from[c] >= 0) {
				grid.setItem(c, inventory.removeItem(from[c], 1));
			}
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player clicker, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index == RESULT) {
			if (!moveItemStackTo(stack, GRID_END, INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, copy);
		} else if (index < GRID_END) {
			if (!moveItemStackTo(stack, GRID_END, INVENTORY_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 1, GRID_END, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == copy.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(clicker, index == RESULT ? copy : stack);
		return copy;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != result && super.canTakeItemForPickAll(stack, slot);
	}

	@Override
	public void removed(Player leaving) {
		super.removed(leaving);
		access.execute((level, pos) -> clearContainer(leaving, grid));
	}

	@Override
	public boolean stillValid(Player viewer) {
		return stillValid(access, viewer, ModBlocks.END_CRAFTING_TABLE);
	}
}
