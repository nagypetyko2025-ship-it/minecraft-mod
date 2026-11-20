package hu.endrealms.gem;

import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModMenus;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Item in the first slot, gem in the second, the result in the third. An item with a gem and an empty gem slot gives the item back without it. */
public class GemTableMenu extends ItemCombinerMenu {
	public static final int ITEM_SLOT = 0;
	public static final int GEM_SLOT = 1;
	public static final int SLOT_Y = 48;

	public GemTableMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public GemTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(ModMenus.GEM_TABLE, id, inventory, access, ItemCombinerMenuSlotDefinition.create()
				.withSlot(ITEM_SLOT, 27, SLOT_Y, stack -> Gems.categoryOf(stack) != null)
				.withSlot(GEM_SLOT, 76, SLOT_Y, stack -> Gems.gemOf(stack) != null)
				.withResultSlot(2, 134, SLOT_Y)
				.build());
	}

	public ItemStack item() {
		return inputSlots.getItem(ITEM_SLOT);
	}

	public ItemStack gem() {
		return inputSlots.getItem(GEM_SLOT);
	}

	/** An item that holds a gem, with nothing to add: the table offers to take the gem out. */
	public boolean removing() {
		return !item().isEmpty() && gem().isEmpty() && !Gems.socketed(item()).isEmpty();
	}

	@Override
	public void createResult() {
		ItemStack item = item();
		if (removing()) {
			ItemStack result = item.copyWithCount(1);
			Gems.clear(result);
			resultSlots.setItem(0, result);
			return;
		}
		Gems.Gem gem = Gems.gemOf(gem());
		if (item.isEmpty() || gem == null || Gems.categoryOf(item) == null || Gems.problem(item, gem) != null) {
			resultSlots.setItem(0, ItemStack.EMPTY);
			return;
		}
		ItemStack result = item.copyWithCount(1);
		Gems.socket(result, gem);
		resultSlots.setItem(0, result);
	}

	@Override
	protected void onTake(Player player, ItemStack stack) {
		stack.onCraftedBy(player, stack.getCount());
		if (!removing() && player instanceof ServerPlayer serverPlayer) {
			AdvancementHolder advancement = serverPlayer.level().getServer().getAdvancements().get(EndRealms.id("gem_socket"));
			if (advancement != null) {
				serverPlayer.getAdvancements().award(advancement, "done");
			}
		}
		inputSlots.removeItem(ITEM_SLOT, 1);
		inputSlots.removeItem(GEM_SLOT, 1);
		access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F));
	}

	@Override
	protected boolean isValidBlock(BlockState state) {
		return state.is(ModBlocks.GEM_TABLE);
	}
}
