package hu.endrealms.brew;

import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModItems;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What the Fermentation Station makes. A recipe takes one to three ingredients, each in its own slot (any of the three, in
 * any order), plus one glass bottle and one bucket of water for every bottle of brandy.
 */
public final class Brewing {
	/** Every brandy's effects, its nausea included, last this long. */
	public static final int DURATION = 20 * 60 * 3;
	public static final int MAX_DRUNK_LEVEL = 3;     // amplifier 3 = Drunkenness IV
	public static final int SLOTS = 3;

	public static final Holder<MobEffect> DRUNKENNESS = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, EndRealms.id("drunkenness"), new DrunkennessEffect());

	/** A brandy's quality: 1 fresh from the station, 2 and 3 after time in an Aging Barrel. Missing means 1. */
	public static final int MAX_QUALITY = 3;
	public static final net.minecraft.core.component.DataComponentType<Integer> QUALITY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			EndRealms.id("brandy_quality"), net.minecraft.core.component.DataComponentType.<Integer>builder()
					.persistent(com.mojang.serialization.Codec.intRange(1, MAX_QUALITY))
					.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());

	public record Input(Item item, int count) {
	}

	public record Brew(List<Input> inputs, Item brandy) {
	}

	/** A recipe that fits what lies in the slots, and how many to take from each slot for one bottle. */
	public record Match(Brew brew, int[] take) {
	}

	private static List<Brew> brews;

	private Brewing() {
	}

	public static void init() {
	}

	public static int quality(ItemStack stack) {
		return Math.max(1, Math.min(MAX_QUALITY, stack.getOrDefault(QUALITY, 1)));
	}

	/** Sets the quality, with the name colour and (at the top) the shimmer that go with it. */
	public static void setQuality(ItemStack stack, int quality) {
		stack.set(QUALITY, quality);
		stack.set(net.minecraft.core.component.DataComponents.RARITY, quality >= MAX_QUALITY ? net.minecraft.world.item.Rarity.RARE
				: quality == 2 ? net.minecraft.world.item.Rarity.UNCOMMON : net.minecraft.world.item.Rarity.COMMON);
		if (quality >= MAX_QUALITY) {
			stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
	}

	public static boolean isBrandy(ItemStack stack) {
		return stack.getItem() instanceof BrandyItem;
	}

	public static List<Brew> brews() {
		if (brews == null) {
			brews = List.of(
					new Brew(List.of(new Input(Items.APPLE, 6)), ModItems.APPLE_BRANDY),
					new Brew(List.of(new Input(Items.GLOW_BERRIES, 12)), ModItems.GLOW_BERRY_BRANDY),
					new Brew(List.of(new Input(Items.SWEET_BERRIES, 12)), ModItems.SWEET_BERRY_BRANDY),
					new Brew(List.of(new Input(ModItems.MOONBELL_NECTAR, 4)), ModItems.NECTAR_BRANDY),
					new Brew(List.of(new Input(ModItems.STARBERRY, 8)), ModItems.STARBERRY_BRANDY),
					new Brew(List.of(new Input(Items.APPLE, 4), new Input(Items.SWEET_BERRIES, 6)), ModItems.FRUIT_MEDLEY_BRANDY),
					new Brew(List.of(new Input(Items.APPLE, 4), new Input(Items.HONEY_BOTTLE, 2)), ModItems.HONEYED_APPLE_BRANDY),
					new Brew(List.of(new Input(Items.GLOW_BERRIES, 8), new Input(ModItems.MOONBELL_NECTAR, 2)), ModItems.GLOWING_NECTAR_BRANDY),
					new Brew(List.of(new Input(Items.CHORUS_FRUIT, 4), new Input(ModItems.STARBERRY, 4)), ModItems.CHORUS_BERRY_BRANDY),
					new Brew(List.of(new Input(Items.MELON_SLICE, 8), new Input(Items.SWEET_BERRIES, 6)), ModItems.MELON_REFRESHER),
					new Brew(List.of(new Input(ModItems.MOONBELL_NECTAR, 3), new Input(ModItems.STARBERRY, 4), new Input(Items.GLOW_BERRIES, 6)), ModItems.ENDLESS_BRANDY),
					new Brew(List.of(new Input(Items.APPLE, 4), new Input(Items.GLOW_BERRIES, 8), new Input(Items.GOLDEN_CARROT, 1)), ModItems.MINERS_BRANDY),
					new Brew(List.of(new Input(ModItems.DRAGON_BONE, 1), new Input(Items.APPLE, 4), new Input(ModItems.MOONBELL_NECTAR, 3)), ModItems.DRAGON_BLOOD_BRANDY),
					new Brew(List.of(new Input(Items.SWEET_BERRIES, 6), new Input(Items.HONEY_BOTTLE, 2), new Input(Items.COCOA_BEANS, 2)), ModItems.FOREST_ELIXIR),
					new Brew(List.of(new Input(Items.CHORUS_FRUIT, 4), new Input(Items.ENDER_EYE, 1), new Input(ModItems.STARBERRY, 3)), ModItems.VOID_ESSENCE));
		}
		return brews;
	}

	/** Whether any recipe uses this item. */
	public static boolean isIngredient(ItemStack stack) {
		for (Brew brew : brews()) {
			for (Input input : brew.inputs()) {
				if (stack.is(input.item())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * The recipe the filled slots make: exactly its ingredients, one kind per slot, each with at least as many as one bottle
	 * takes. Anything extra in the slots (another kind, or the same kind split over two slots) matches nothing.
	 */
	public static Match match(List<ItemStack> slots) {
		int filled = 0;
		for (ItemStack stack : slots) {
			if (!stack.isEmpty()) {
				filled++;
			}
		}
		if (filled == 0) {
			return null;
		}
		for (Brew brew : brews()) {
			if (brew.inputs().size() != filled) {
				continue;
			}
			int[] take = new int[slots.size()];
			boolean ok = true;
			for (Input input : brew.inputs()) {
				int found = -1;
				for (int i = 0; i < slots.size(); i++) {
					if (take[i] == 0 && slots.get(i).is(input.item())) {
						found = i;
						break;
					}
				}
				if (found < 0 || slots.get(found).getCount() < input.count()) {
					ok = false;
					break;
				}
				take[found] = input.count();
			}
			if (ok) {
				return new Match(brew, take);
			}
		}
		return null;
	}

	/** The recipe the slots are meant for, even if there is not yet enough of something: for the window's hints. */
	public static Brew intended(List<ItemStack> slots) {
		java.util.Set<Item> kinds = new java.util.HashSet<>();
		for (ItemStack stack : slots) {
			if (!stack.isEmpty() && !kinds.add(stack.getItem())) {
				return null;                     // one kind split over two slots is no recipe, as in match()
			}
		}
		for (Brew brew : brews()) {
			java.util.Set<Item> wanted = new java.util.HashSet<>();
			brew.inputs().forEach(input -> wanted.add(input.item()));
			if (wanted.equals(kinds)) {
				return brew;
			}
		}
		return null;
	}
}
