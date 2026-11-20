package hu.endrealms.mixin;

import hu.endrealms.bag.Bags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each player owns a Backpack container, saved with the player and dropped on death like the rest of the inventory. */
@Mixin(Player.class)
public abstract class PlayerBagMixin implements Bags.Holder {
	@Unique
	private static final String KEY = "EndRealmsBag";

	@Unique
	private SimpleContainer endrealms$bag;

	@Override
	public SimpleContainer endrealms$bag() {
		if (endrealms$bag == null) {          // the inventory menu asks for it while the player is still being constructed
			endrealms$bag = new SimpleContainer(Bags.SIZE);
		}
		return endrealms$bag;
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void endrealms$saveBag(ValueOutput output, CallbackInfo ci) {
		ContainerHelper.saveAllItems(output.child(KEY), endrealms$bag().getItems());
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void endrealms$loadBag(ValueInput input, CallbackInfo ci) {
		ContainerHelper.loadAllItems(input.childOrEmpty(KEY), endrealms$bag().getItems());
	}

	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void endrealms$dropBag(ServerLevel level, CallbackInfo ci) {
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		SimpleContainer bag = endrealms$bag();
		for (int i = 0; i < bag.getContainerSize(); i++) {
			ItemStack stack = bag.removeItemNoUpdate(i);
			if (!stack.isEmpty()) {
				((Entity) (Object) this).spawnAtLocation(level, stack);
			}
		}
	}
}
