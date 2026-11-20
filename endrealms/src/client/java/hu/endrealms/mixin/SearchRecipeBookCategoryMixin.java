package hu.endrealms.mixin;

import hu.endrealms.registry.ModRecipeBook;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the mod's recipes in the "all recipes" tab of the crafting book as well. */
@Mixin(SearchRecipeBookCategory.class)
public class SearchRecipeBookCategoryMixin {
	@Inject(method = "includedCategories", at = @At("RETURN"), cancellable = true)
	private void endrealms$include(CallbackInfoReturnable<List<RecipeBookCategory>> cir) {
		if ((Object) this == SearchRecipeBookCategory.CRAFTING && !cir.getReturnValue().contains(ModRecipeBook.END_REALMS)) {
			List<RecipeBookCategory> all = new ArrayList<>(cir.getReturnValue());
			all.add(ModRecipeBook.END_REALMS);
			cir.setReturnValue(all);
		}
	}
}
