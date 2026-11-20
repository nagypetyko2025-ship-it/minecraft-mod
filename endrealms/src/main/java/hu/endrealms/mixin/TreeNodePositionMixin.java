package hu.endrealms.mixin;

import hu.endrealms.advancement.AdvancementLayout;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.TreeNodePosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the game's rightward layout on the End Realms tab. */
@Mixin(TreeNodePosition.class)
public class TreeNodePositionMixin {
	@Inject(method = "run", at = @At("TAIL"))
	private static void endrealms$layout(AdvancementNode root, CallbackInfo ci) {
		AdvancementLayout.apply(root);
	}
}
