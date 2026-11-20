package hu.endrealms.mail;

import hu.endrealms.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The mail window: the inbox (take only), one slot for what to send, and the player's inventory. */
public class PhaseMailboxMenu extends AbstractContainerMenu {
	public static final int INBOX = PhaseMailboxBlockEntity.SIZE;
	public static final int SEND_SLOT = INBOX;
	public static final int SEND_X = 182;
	public static final int SEND_Y = 131;
	private static final int PLAYER_START = INBOX + 1;
	private static final int PLAYER_END = PLAYER_START + 36;

	private final Container inbox;
	private final SimpleContainer outgoing = new SimpleContainer(1);
	private final BlockPos pos;

	/** Client side: the contents arrive through the usual slot sync. */
	public PhaseMailboxMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(INBOX), BlockPos.ZERO);
	}

	public PhaseMailboxMenu(int id, Inventory inventory, PhaseMailboxBlockEntity mailbox) {
		this(id, inventory, mailbox, mailbox.getBlockPos());
	}

	private PhaseMailboxMenu(int id, Inventory inventory, Container inbox, BlockPos pos) {
		super(ModMenus.PHASE_MAILBOX, id);
		this.inbox = inbox;
		this.pos = pos;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inbox, col + row * 9, 8 + col * 18, 18 + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return false;
					}
				});
			}
		}
		addSlot(new Slot(outgoing, 0, SEND_X, SEND_Y));
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, 142));
		}
	}

	public BlockPos pos() {
		return pos;
	}

	SimpleContainer outgoing() {
		return outgoing;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		boolean moved = index < PLAYER_START ? moveItemStackTo(stack, PLAYER_START, PLAYER_END, true) : moveItemStackTo(stack, SEND_SLOT, SEND_SLOT + 1, false);
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
		return inbox.stillValid(player);
	}

	/** Anything left in the send slot goes back to the player. */
	@Override
	public void removed(Player player) {
		super.removed(player);
		clearContainer(player, outgoing);
	}
}
