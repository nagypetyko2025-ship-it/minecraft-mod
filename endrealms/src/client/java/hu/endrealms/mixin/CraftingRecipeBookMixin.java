package hu.endrealms.mixin;

import hu.endrealms.registry.ModItems;
import hu.endrealms.registry.ModRecipeBook;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the End Realms tab to the crafting recipe book. */
@Mixin(CraftingRecipeBookComponent.class)
public class CraftingRecipeBookMixin {
	@Shadow
	@Final
	@Mutable
	private static List<RecipeBookComponent.TabInfo> TABS;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void endrealms$addTab(CallbackInfo ci) {
		List<RecipeBookComponent.TabInfo> tabs = new ArrayList<>(TABS);
		tabs.add(new RecipeBookComponent.TabInfo(ModItems.CHORUSITE_INGOT, ModRecipeBook.END_REALMS));
		TABS = List.copyOf(tabs);
	}
}
