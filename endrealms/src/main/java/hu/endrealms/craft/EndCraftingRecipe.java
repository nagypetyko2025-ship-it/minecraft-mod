package hu.endrealms.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import hu.endrealms.registry.ModRecipeBook;

/**
 * A shaped recipe of up to 5x5 for the End Crafting Table. JSON: {"type": "endrealms:end_crafting", "pattern": [...],
 * "key": {...}, "result": {...}}, like a vanilla shaped recipe but with rows of up to five. Mirrored shapes match too.
 */
public record EndCraftingRecipe(List<String> rows, Map<Character, Ingredient> key, ShapedRecipePattern pattern, ItemStackTemplate result) implements Recipe<CraftingInput> {
	public static final int SIZE = 5;

	private static final Codec<Character> SYMBOL = Codec.STRING.comapFlatMap(
			s -> s.length() == 1 && !s.equals(" ") ? DataResult.success(s.charAt(0)) : DataResult.error(() -> "Key must be one character, not '" + s + "'"),
			String::valueOf);

	public static final MapCodec<EndCraftingRecipe> MAP_CODEC = RecordCodecBuilder.<EndCraftingRecipe>mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("pattern").forGetter(EndCraftingRecipe::rows),
			Codec.unboundedMap(SYMBOL, Ingredient.CODEC).fieldOf("key").forGetter(EndCraftingRecipe::key),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(EndCraftingRecipe::result)
	).apply(i, EndCraftingRecipe::create)).validate(EndCraftingRecipe::check);

	public static final StreamCodec<RegistryFriendlyByteBuf, EndCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
			ShapedRecipePattern.STREAM_CODEC, EndCraftingRecipe::pattern,
			ItemStackTemplate.STREAM_CODEC, EndCraftingRecipe::result,
			(pattern, result) -> new EndCraftingRecipe(List.of(), Map.of(), pattern, result));

	private static EndCraftingRecipe create(List<String> rows, Map<Character, Ingredient> key, ItemStackTemplate result) {
		ShapedRecipePattern pattern;
		try {
			pattern = ShapedRecipePattern.of(key, rows);
		} catch (RuntimeException exception) {
			pattern = null;     // reported by check()
		}
		return new EndCraftingRecipe(rows, key, pattern, result);
	}

	private static DataResult<EndCraftingRecipe> check(EndCraftingRecipe recipe) {
		if (recipe.rows.isEmpty() || recipe.rows.size() > SIZE || recipe.rows.stream().anyMatch(row -> row.isEmpty() || row.length() > SIZE)) {
			return DataResult.error(() -> "End crafting patterns are 1 to 5 rows of 1 to 5 symbols");
		}
		for (String row : recipe.rows) {
			for (char c : row.toCharArray()) {
				if (c != ' ' && !recipe.key.containsKey(c)) {
					return DataResult.error(() -> "Symbol '" + c + "' is not in the key");
				}
			}
		}
		return recipe.pattern == null ? DataResult.error(() -> "Invalid pattern " + recipe.rows) : DataResult.success(recipe);
	}

	@Override
	public boolean matches(CraftingInput input, net.minecraft.world.level.Level level) {
		return pattern.matches(input);
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		return result.create();
	}

	/** Kept out of the vanilla recipe book: the table shows these itself. */
	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<EndCraftingRecipe> getSerializer() {
		return EndCrafting.SERIALIZER;
	}

	@Override
	public RecipeType<EndCraftingRecipe> getType() {
		return EndCrafting.TYPE;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return ModRecipeBook.END_REALMS;
	}

	/** The ingredients laid out on the 5x5 grid as the table places them: centred. Empty where nothing goes. */
	public List<Optional<Ingredient>> grid() {
		int left = (SIZE - pattern.width()) / 2;
		int top = (SIZE - pattern.height()) / 2;
		java.util.ArrayList<Optional<Ingredient>> cells = new java.util.ArrayList<>();
		for (int i = 0; i < SIZE * SIZE; i++) {
			cells.add(Optional.empty());
		}
		for (int y = 0; y < pattern.height(); y++) {
			for (int x = 0; x < pattern.width(); x++) {
				cells.set((top + y) * SIZE + left + x, pattern.ingredients().get(y * pattern.width() + x));
			}
		}
		return cells;
	}
}
