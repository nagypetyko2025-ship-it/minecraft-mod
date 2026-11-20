package hu.endrealms.mixin;

import hu.endrealms.registry.ModRecipeBook;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Files shaped recipes of the mod's items under the End Realms recipe book tab. */
@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
	@Shadow
	@Final
	private ItemStackTemplate result;

	public RecipeBookCategory recipeBookCategory() {
		return ModRecipeBook.categoryFor(result, ((NormalCraftingRecipe) (Object) this).category());
	}
}
