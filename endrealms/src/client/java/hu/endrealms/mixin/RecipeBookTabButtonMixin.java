package hu.endrealms.mixin;

import hu.endrealms.registry.ModRecipeBook;
import java.util.Set;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The crafting tabs stay on screen even when empty: moving the mod's recipes away must not make the vanilla ones vanish. */
@Mixin(RecipeBookTabButton.class)
public abstract class RecipeBookTabButtonMixin {
	@Shadow
	public abstract ExtendedRecipeBookCategory getCategory();

	@Inject(method = "updateVisibility", at = @At("RETURN"), cancellable = true)
	private void endrealms$alwaysVisible(ClientRecipeBook book, CallbackInfoReturnable<Boolean> cir) {
		ExtendedRecipeBookCategory category = getCategory();
		if (Set.of(RecipeBookCategories.CRAFTING_EQUIPMENT, RecipeBookCategories.CRAFTING_BUILDING_BLOCKS, RecipeBookCategories.CRAFTING_MISC,
				RecipeBookCategories.CRAFTING_REDSTONE, ModRecipeBook.END_REALMS).contains(category)) {
			((AbstractWidget) (Object) this).visible = true;
			cir.setReturnValue(true);
		}
	}
}
