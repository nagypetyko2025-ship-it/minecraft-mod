package hu.endrealms.brew;

import hu.endrealms.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Fermentation Station: up to three ingredients, glass bottles, and water poured in with buckets (up to ten). Nothing
 * happens until someone presses Start in its window; from then on it brews bottle after bottle, every one taking one bucket
 * of water and one bottle, for as long as the ingredients make a recipe (see {@link Brewing}), then stops by itself.
 * Hoppers fill ingredients from above and bottles from the sides, and take the brandy from below.
 */
public class FermentationStationBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
	public static final int INGREDIENTS = 0;                     // slots 0, 1, 2
	public static final int BOTTLES = Brewing.SLOTS;
	public static final int OUTPUT = Brewing.SLOTS + 1;
	public static final int SIZE = Brewing.SLOTS + 2;
	public static final int MAX_WATER = 10;
	/** Five minutes for every bottle. */
	public static final int BREW_TICKS = 20 * 60 * 5;
	private static final int[] TOP = {0, 1, 2};
	private static final int[] SIDES = {BOTTLES};
	private static final int[] BOTTOM = {OUTPUT};

	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private int progress;
	private int water;
	private boolean running;

	/** Synced to an open window: progress, its total, water, and whether it runs. */
	final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> progress;
				case 1 -> BREW_TICKS;
				case 2 -> water;
				default -> running ? 1 : 0;
			};
		}

		@Override
		public void set(int index, int value) {
			switch (index) {
				case 0 -> progress = value;
				case 2 -> water = value;
				case 3 -> running = value != 0;
				default -> {
				}
			}
		}

		@Override
		public int getCount() {
			return 4;
		}
	};

	public FermentationStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FERMENTATION_STATION, pos, state);
	}

	public int water() {
		return water;
	}

	/** One bucket in; false when it is full. */
	public boolean addWater() {
		if (water >= MAX_WATER) {
			return false;
		}
		water++;
		setChanged();
		return true;
	}

	/** One bucket back out; false when there is none. */
	public boolean takeWater() {
		if (water <= 0) {
			return false;
		}
		water--;
		setChanged();
		return true;
	}

	private List<ItemStack> ingredients() {
		return items.subList(INGREDIENTS, INGREDIENTS + Brewing.SLOTS);
	}

	/** The recipe it can brew one more bottle of right now, or null. */
	private Brewing.Match ready() {
		Brewing.Match match = Brewing.match(ingredients());
		if (match == null || water < 1 || !items.get(BOTTLES).is(Items.GLASS_BOTTLE)) {
			return null;
		}
		ItemStack out = items.get(OUTPUT);
		return out.isEmpty() || out.is(match.brew().brandy()) && out.getCount() < out.getMaxStackSize() ? match : null;
	}

	/** The Start / Stop button. Start only does something when there is a whole bottle's worth of everything. */
	void toggle() {
		if (running) {
			running = false;
			progress = 0;
		} else if (ready() != null) {
			running = true;
		}
		setChanged();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FermentationStationBlockEntity station) {
		if (station.running) {
			Brewing.Match match = station.ready();
			if (match == null) {
				station.running = false;      // ran out of something, or someone took it away: it starts over next time
				station.progress = 0;
			} else if (++station.progress >= BREW_TICKS) {
				station.progress = 0;
				for (int i = 0; i < Brewing.SLOTS; i++) {
					station.items.get(INGREDIENTS + i).shrink(match.take()[i]);
				}
				station.items.get(BOTTLES).shrink(1);
				station.water--;
				ItemStack out = station.items.get(OUTPUT);
				if (out.isEmpty()) {
					station.items.set(OUTPUT, new ItemStack(match.brew().brandy()));
				} else {
					out.grow(1);
				}
				level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
				if (station.ready() == null) {
					station.running = false;
				}
			}
			station.setChanged();
		}
		if (state.getValue(FermentationStationBlock.FERMENTING) != station.running) {
			level.setBlock(pos, state.setValue(FermentationStationBlock.FERMENTING, station.running), Block.UPDATE_ALL);
		}
	}

	// ---------------------------------------------------------------- container

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
		if (!taken.isEmpty()) {
			setChanged();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < BOTTLES ? Brewing.isIngredient(stack) : slot == BOTTLES && stack.is(Items.GLASS_BOTTLE);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
	}

	/** A hopper keeps each kind in one slot: the slot that already has it, or else the first empty one. */
	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		if (!canPlaceItem(slot, stack)) {
			return false;
		}
		if (slot >= BOTTLES) {
			return true;
		}
		int home = -1;
		for (int i = 0; i < Brewing.SLOTS && home < 0; i++) {
			if (items.get(i).is(stack.getItem())) {
				home = i;
			}
		}
		for (int i = 0; i < Brewing.SLOTS && home < 0; i++) {
			if (items.get(i).isEmpty()) {
				home = i;
			}
		}
		return slot == home;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT && side == Direction.DOWN;
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
		setChanged();
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.endrealms.fermentation_station");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new FermentationStationMenu(id, inventory, this, data);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			Containers.dropContents(level, pos, this);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("Progress", progress);
		output.putInt("Water", water);
		output.putBoolean("Running", running);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		progress = input.getIntOr("Progress", 0);
		water = Math.max(0, Math.min(MAX_WATER, input.getIntOr("Water", 0)));
		running = input.getBooleanOr("Running", false);
	}
}
