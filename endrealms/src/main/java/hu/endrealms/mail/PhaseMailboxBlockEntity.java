package hu.endrealms.mail;

import hu.endrealms.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Phase Mailbox: a named inbox. Mail only ever arrives through the network; players and hoppers can take it out, but
 * nothing can be put in by hand, so whatever lies inside was sent to this name.
 */
public class PhaseMailboxBlockEntity extends BlockEntity implements Container, MenuProvider {
	public static final int SIZE = 27;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private String name = "";
	private UUID owner;
	private String ownerName = "";

	public PhaseMailboxBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PHASE_MAILBOX, pos, state);
	}

	public String name() {
		return name;
	}

	public UUID owner() {
		return owner;
	}

	public String ownerName() {
		return ownerName;
	}

	void setIdentity(String name, UUID owner, String ownerName) {
		this.name = name;
		this.owner = owner;
		this.ownerName = ownerName;
		setChanged();
	}

	/** Puts as much of the stack in as fits; returns how many went in. */
	int deliver(ItemStack stack) {
		int before = stack.getCount();
		for (int slot = 0; slot < SIZE && !stack.isEmpty(); slot++) {
			ItemStack here = items.get(slot);
			if (!here.isEmpty() && ItemStack.isSameItemSameComponents(here, stack) && here.getCount() < here.getMaxStackSize()) {
				int moved = Math.min(stack.getCount(), here.getMaxStackSize() - here.getCount());
				here.grow(moved);
				stack.shrink(moved);
			}
		}
		for (int slot = 0; slot < SIZE && !stack.isEmpty(); slot++) {
			if (items.get(slot).isEmpty()) {
				items.set(slot, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
			}
		}
		setChanged();
		return before - stack.getCount();
	}

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

	/** Hoppers and players may take mail out, never put anything in. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
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
		return Component.translatable("container.endrealms.phase_mailbox");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new PhaseMailboxMenu(id, inventory, this);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			Containers.dropContents(level, pos, this);
			PhaseMail.unregister(this);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putString("MailName", name);
		output.putString("OwnerName", ownerName);
		if (owner != null) {
			output.store("Owner", UUIDUtil.CODEC, owner);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		name = input.getStringOr("MailName", "");
		ownerName = input.getStringOr("OwnerName", "");
		owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
	}
}
