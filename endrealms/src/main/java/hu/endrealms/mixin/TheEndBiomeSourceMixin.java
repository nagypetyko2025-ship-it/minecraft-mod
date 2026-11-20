package hu.endrealms.mixin;

import hu.endrealms.registry.ModWorldgen;
import hu.endrealms.worldgen.BasinSites;
import hu.endrealms.worldgen.GraveyardIslandFeature;
import hu.endrealms.worldgen.ScorchedIslandFeature;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Puts the Astral Basin and Scorched Barrens biomes exactly on the islands their features build. */
@Mixin(TheEndBiomeSource.class)
public class TheEndBiomeSourceMixin {
	@Unique
	private static HolderGetter<Biome> endrealms$lookup;

	@Unique
	private Holder<Biome> endrealms$basin;

	@Unique
	private Holder<Biome> endrealms$scorched;

	@Unique
	private Holder<Biome> endrealms$graveyard;

	@Inject(method = "create", at = @At("HEAD"))
	private static void endrealms$captureLookup(HolderGetter<Biome> biomes, CallbackInfoReturnable<TheEndBiomeSource> cir) {
		endrealms$lookup = biomes;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void endrealms$init(Holder<Biome> end, Holder<Biome> highlands, Holder<Biome> midlands, Holder<Biome> islands,
			Holder<Biome> barrens, CallbackInfo ci) {
		if (endrealms$lookup != null) {
			this.endrealms$basin = endrealms$lookup.get(ModWorldgen.ASTRAL_BASIN).orElse(null);
			this.endrealms$scorched = endrealms$lookup.get(ModWorldgen.SCORCHED_BARRENS).orElse(null);
			this.endrealms$graveyard = endrealms$lookup.get(ModWorldgen.DRAGON_GRAVEYARD).orElse(null);
		}
	}

	@Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true)
	private void endrealms$basinBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
			CallbackInfoReturnable<Holder<Biome>> cir) {
		int x = quartX * 4 + 2;
		int z = quartZ * 4 + 2;
		if (this.endrealms$basin != null && BasinSites.isBasinBiome(x, z)) {
			cir.setReturnValue(this.endrealms$basin);
		} else if (this.endrealms$scorched != null && ScorchedIslandFeature.Sites.isScorchedBiome(x, z)) {
			cir.setReturnValue(this.endrealms$scorched);
		} else if (this.endrealms$graveyard != null && GraveyardIslandFeature.Sites.isGraveyardBiome(x, z)) {
			cir.setReturnValue(this.endrealms$graveyard);
		}
	}
}
