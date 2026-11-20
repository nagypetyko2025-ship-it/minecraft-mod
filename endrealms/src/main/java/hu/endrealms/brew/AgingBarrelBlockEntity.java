package hu.endrealms.brew;

import hu.endrealms.registry.ModBlockEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Aging Barrel: three slots, one bottle of brandy in each. A bottle left in it long enough turns from quality 1 to 2, and
 * later from 2 to 3 (see {@link Brewing#QUALITY}). Each slot keeps its own time; taking a bottle out starts it over.
 * It only ages while its chunk is loaded. Hoppers put brandy in from above and the sides and take out only finished
 * (quality 3) bottles from below.
 */
public class AgingBarrelBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
	public static final int SIZE = 3;
	/** Ticks to the next quality, by the quality the bottle has now: one Minecraft day to 2, then a day and a half to 3. */
	public static final int[] AGE_TICKS = {0, 24000, 36000};
	private static final int[] ALL = {0, 1, 2};

	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private final int[] age = new int[SIZE];

	/** Synced to an open window: how far each slot is to its next quality, in thousandths, and the ticks left (in seconds). */
	final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			int slot = index % SIZE;
			int total = total(slot);
			if (total <= 0) {
				return 0;
			}
			return index < SIZE ? Math.min(1000, age[slot] * 1000 / total) : (total - age[slot]) / 20;
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return SIZE * 2;
		}
	};

	public AgingBarrelBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.AGING_BARREL, pos, state);
	}

	/** Ticks this slot needs for its next quality, or 0 when there is nothing (more) to age. */
	private int total(int slot) {
		ItemStack stack = items.get(slot);
		if (!Brewing.isBrandy(stack)) {
			return 0;
		}
		int quality = Brewing.quality(stack);
		return quality >= Brewing.MAX_QUALITY ? 0 : AGE_TICKS[quality];
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, AgingBarrelBlockEntity barrel) {
		boolean changed = false;
		for (int slot = 0; slot < SIZE; slot++) {
			int total = barrel.total(slot);
			if (total <= 0) {
				continue;
			}
			changed = true;
			if (++barrel.age[slot] >= total) {
				barrel.age[slot] = 0;
				ItemStack stack = barrel.items.get(slot);
				Brewing.setQuality(stack, Brewing.quality(stack) + 1);
				level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 0.7F);
			}
		}
		if (changed && level.getGameTime() % 100 == 0) {
			barrel.setChanged();          // the ages are saved every few seconds, not every tick
		}
	}

	// ---------------------------------------------------------------- container

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
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
			if (items.get(slot).isEmpty()) {
				age[slot] = 0;
			}
			setChanged();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		age[slot] = 0;
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (!ItemStack.isSameItemSameComponents(items.get(slot), stack)) {
			age[slot] = 0;                // another bottle: its time starts over
		}
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return Brewing.isBrandy(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return ALL;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side != Direction.DOWN && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN && Brewing.quality(stack) >= Brewing.MAX_QUALITY;
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
		java.util.Arrays.fill(age, 0);
		setChanged();
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.endrealms.aging_barrel");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new AgingBarrelMenu(id, inventory, this, data);
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
		output.putIntArray("Age", age.clone());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		int[] saved = input.getIntArray("Age").orElse(new int[0]);
		for (int i = 0; i < SIZE; i++) {
			age[i] = i < saved.length ? Math.max(0, saved[i]) : 0;
		}
	}
}
