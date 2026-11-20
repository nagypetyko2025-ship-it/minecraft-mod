package hu.endrealms.mixin;

import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** A bobber floating on astral water reels in only the End fish: no vanilla fish, junk or treasure. */
@Mixin(FishingHook.class)
public class FishingHookMixin {
	@Unique
	private static final ResourceKey<LootTable> ASTRAL_FISHING =
			ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("gameplay/astral_fishing"));

	@ModifyArg(method = "retrieve", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/ReloadableServerRegistries$Holder;getLootTable(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/storage/loot/LootTable;"))
	private ResourceKey<LootTable> endrealms$astralFishing(ResourceKey<LootTable> original) {
		FishingHook hook = (FishingHook) (Object) this;
		BlockPos pos = hook.blockPosition();
		// the bobber rides on the surface, so its own block can be the air just above the water
		boolean astral = hook.level().getFluidState(pos).getType().isSame(ModFluids.ASTRAL_WATER)
				|| hook.level().getFluidState(pos.below()).getType().isSame(ModFluids.ASTRAL_WATER);
		return astral ? ASTRAL_FISHING : original;
	}
}
