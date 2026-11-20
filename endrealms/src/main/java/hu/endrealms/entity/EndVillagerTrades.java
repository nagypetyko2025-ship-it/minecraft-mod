package hu.endrealms.entity;

import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModFluids;
import hu.endrealms.registry.ModItems;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

/**
 * What each End villager sells, for chorusite shards. They never pay shards out: that made the ore pointless to mine.
 */
final class EndVillagerTrades {
	private static final ItemLike CURRENCY = ModItems.CHORUSITE_SHARD;
	private static final int SELL_USES = 12;
	private static final int XP = 3;
	private static final float PRICE_MULTIPLIER = 0.05F;

	private EndVillagerTrades() {
	}

	/** The villager gives count x item for price chorusite shards. */
	private static MerchantOffer sell(int price, ItemLike item, int count) {
		return new MerchantOffer(new ItemCost(CURRENCY, price), Optional.empty(), new ItemStack(item, count), SELL_USES, XP, PRICE_MULTIPLIER);
	}

	/** Salvage: scarce goods, sold only by the castaways of the Sky Wrecks. */
	private static MerchantOffer salvage(int price, ItemLike item) {
		return new MerchantOffer(new ItemCost(CURRENCY, price), Optional.empty(), new ItemStack(item), 2, XP * 4, PRICE_MULTIPLIER);
	}

	static void fill(EndVillager.Profession profession, MerchantOffers offers) {
		switch (profession) {
			case CASTAWAY -> {
				offers.add(salvage(18, ModItems.PHASITE_SHARD));
				offers.add(salvage(36, ModItems.EMBER_GARNET));
				offers.add(salvage(36, ModItems.ASH_QUARTZ));
				offers.add(salvage(36, ModItems.BLOODSTONE));
				offers.add(salvage(28, Items.NETHERITE_SCRAP));
			}
			case TOOLSMITH -> {
				offers.add(sell(12, ModItems.CHORUSITE_UPGRADE_SMITHING_TEMPLATE, 1));
				offers.add(sell(6, Items.SHULKER_SHELL, 1));
				offers.add(sell(1, Items.END_ROD, 4));
			}
			case MASON -> {
				offers.add(sell(1, ModBlocks.VOIDSTONE_BRICKS, 16));
				offers.add(sell(1, ModBlocks.POLISHED_VOIDSTONE, 16));
				offers.add(sell(1, ModBlocks.VOID_SANDSTONE, 16));
				offers.add(sell(1, ModBlocks.CRYSTALLINE_VOIDSTONE, 8));
			}
			case BUTCHER -> {
				offers.add(sell(1, ModItems.RAW_GLIMMERHOOF, 8));
				offers.add(sell(1, ModItems.COOKED_GLIMMERHOOF, 6));
			}
			case FISHERMAN -> {
				offers.add(sell(1, ModItems.COOKED_STARFIN, 6));
				offers.add(sell(1, ModItems.COOKED_VOID_EEL, 6));
				offers.add(sell(1, ModItems.COOKED_GLOWGILL, 4));
				offers.add(sell(2, ModFluids.ASTRAL_WATER_BUCKET, 1));
			}
			case CLERIC -> {
				offers.add(sell(1, Items.ENDER_PEARL, 4));
				offers.add(sell(1, Items.POPPED_CHORUS_FRUIT, 8));
				offers.add(sell(3, Items.DRAGON_BREATH, 1));
			}
			case LIBRARIAN -> {
				offers.add(sell(1, Items.ENDER_EYE, 2));
				offers.add(sell(3, Items.END_CRYSTAL, 1));
				offers.add(sell(1, Items.EXPERIENCE_BOTTLE, 4));
			}
			case FORESTER -> {
				offers.add(sell(1, ModBlocks.ASTRAL_LOG, 16));
				offers.add(sell(1, ModBlocks.ASTRAL_SAPLING, 2));
				offers.add(sell(1, ModBlocks.END_SOIL, 16));
				offers.add(sell(1, ModBlocks.VOIDBLOOM, 4));
			}
			case JEWELER -> {
				offers.add(sell(1, ModItems.PRISM_SHARD, 8));
				offers.add(sell(1, ModBlocks.PRISM_CRYSTAL_BLOCK, 1));
				offers.add(sell(1, ModBlocks.PRISM_LAMP, 2));
				offers.add(sell(2, Items.AMETHYST_SHARD, 8));
			}
		}
	}
}
