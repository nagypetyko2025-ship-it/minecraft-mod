package hu.endrealms.mixin;

import hu.endrealms.registry.ModRecipeBook;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Files shapeless recipes of the mod's items under the End Realms recipe book tab. */
@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
	@Shadow
	@Final
	private ItemStackTemplate result;

	public RecipeBookCategory recipeBookCategory() {
		return ModRecipeBook.categoryFor(result, ((NormalCraftingRecipe) (Object) this).category());
	}
}
