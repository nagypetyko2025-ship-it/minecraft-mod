package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;

/** The End Realms tab of the crafting recipe book: every crafting recipe that makes one of the mod's own items. */
public final class ModRecipeBook {
	public static final RecipeBookCategory END_REALMS = Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, EndRealms.id("end_realms"), new RecipeBookCategory());

	private ModRecipeBook() {
	}

	public static void init() {
	}

	public static RecipeBookCategory categoryFor(ItemStackTemplate result, CraftingBookCategory category) {
		String path = BuiltInRegistries.ITEM.getKey(result.item().value()).getPath();
		if (path.equals("canvas") || path.equals("large_canvas") || path.equals("fermentation_station") || path.equals("aging_barrel")) {
			return RecipeBookCategories.CRAFTING_MISC;            // nothing to do with the End: they stay with the ordinary recipes
		}
		if (BuiltInRegistries.ITEM.getKey(result.item().value()).getNamespace().equals(EndRealms.MOD_ID)) {
			return END_REALMS;
		}
		return switch (category) {
			case BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
			case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
			case REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
			case MISC -> RecipeBookCategories.CRAFTING_MISC;
		};
	}
}
