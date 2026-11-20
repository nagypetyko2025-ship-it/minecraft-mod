package hu.endrealms.mixin;

import hu.endrealms.gem.Gems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Smithing a netherite item into Chorusite keeps its gem, but the gem's bonuses must add to the new item's stats. */
@Mixin(TransmuteRecipe.class)
public class TransmuteRecipeMixin {
	@Inject(method = "createWithOriginalComponents(Lnet/minecraft/world/item/ItemStackTemplate;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
	private static void endrealms$rebuildGems(ItemStackTemplate template, ItemStack original, int extra, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		if (!Gems.socketed(result).isEmpty()) {
			Gems.rebuild(result);
		}
	}
}
