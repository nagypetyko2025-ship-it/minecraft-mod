package hu.endrealms.mixin;

import hu.endrealms.bag.Bags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Respawning and leaving the End create a new player object; whatever death did not drop moves over to it. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerBagMixin {
	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void endrealms$keepBag(ServerPlayer old, boolean alive, CallbackInfo ci) {
		SimpleContainer from = Bags.of(old);
		SimpleContainer to = Bags.of((ServerPlayer) (Object) this);
		for (int i = 0; i < from.getContainerSize(); i++) {
			to.setItem(i, from.getItem(i).copy());
		}
	}
}
