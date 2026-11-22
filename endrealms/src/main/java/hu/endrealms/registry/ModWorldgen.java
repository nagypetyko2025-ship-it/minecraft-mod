package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import hu.endrealms.worldgen.AstralBasinFeature;
import hu.endrealms.worldgen.AstralPlantFeatures;
import hu.endrealms.worldgen.DeepglowFeature;
import hu.endrealms.worldgen.GraveyardFeatures;
import hu.endrealms.worldgen.PhasiteVeinFeature;
import hu.endrealms.worldgen.PrismFeatures;
import hu.endrealms.worldgen.ScorchedFeatures;
import hu.endrealms.worldgen.ScorchedIslandFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.TheEndBiomes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** The ore features themselves are JSON (data/endrealms/worldgen); this only injects them into End biomes. */
public final class ModWorldgen {
	public static final ResourceKey<PlacedFeature> ORE_CHORUSITE =
			ResourceKey.create(Registries.PLACED_FEATURE, EndRealms.id("ore_chorusite"));

	/** Phasite veins on the underside of the islands. */
	public static final ResourceKey<PlacedFeature> PHASITE_VEIN =
			ResourceKey.create(Registries.PLACED_FEATURE, EndRealms.id("phasite_vein"));

	/** Small end-soil patches in every End biome except the Astral Forest (whose whole surface is end soil). */
	public static final ResourceKey<PlacedFeature> END_SOIL_PATCH =
			ResourceKey.create(Registries.PLACED_FEATURE, EndRealms.id("end_soil_patch"));

	/** Voidbloom patches; the simple_block feature only places it where it survives, i.e. on end soil. */
	public static final ResourceKey<PlacedFeature> PATCH_VOIDBLOOM =
			ResourceKey.create(Registries.PLACED_FEATURE, EndRealms.id("patch_voidbloom"));

	/** Astral Forest: tall thin purple trees on the big outer End islands (highlands + midlands). */
	public static final ResourceKey<Biome> ASTRAL_FOREST =
			ResourceKey.create(Registries.BIOME, EndRealms.id("astral_forest"));

	/** Astral Basin: big island with a mountain ring around a lake of astral water (see BasinSites). */
	public static final ResourceKey<Biome> ASTRAL_BASIN =
			ResourceKey.create(Registries.BIOME, EndRealms.id("astral_basin"));

	/** Prism Wastes: pale, barren big islands bristling with turquoise crystal spires. */
	public static final ResourceKey<Biome> PRISM_WASTES =
			ResourceKey.create(Registries.BIOME, EndRealms.id("prism_wastes"));

	/** Scorched Barrens: its own hilly islands under a blanket of ash, dotted with small volcanoes (see ScorchedIslandFeature). */
	public static final ResourceKey<Biome> SCORCHED_BARRENS =
			ResourceKey.create(Registries.BIOME, EndRealms.id("scorched_barrens"));

	/** Dragon Graveyard: pale islands with giant ribcages, skulls and trees of bone. */
	public static final ResourceKey<Biome> DRAGON_GRAVEYARD =
			ResourceKey.create(Registries.BIOME, EndRealms.id("dragon_graveyard"));

	private ModWorldgen() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("astral_basin"), AstralBasinFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("prism_spire"), PrismFeatures.Spire.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("prism_patch"), PrismFeatures.Patch.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("phasite_vein"), PhasiteVeinFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("starreed_patch"), AstralPlantFeatures.Starreed.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("moonbell_patch"), AstralPlantFeatures.Moonbell.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("ash_volcano"), ScorchedFeatures.Volcano.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("deepglow_patch"), DeepglowFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("scorched_island"), ScorchedIslandFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("ash_blanket"), ScorchedFeatures.AshBlanket.CODEC);

		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("graveyard_island"), hu.endrealms.worldgen.GraveyardIslandFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("bone_soil_blanket"), GraveyardFeatures.Blanket.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("dragon_ribcage"), GraveyardFeatures.Ribcage.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("dragon_skull"), GraveyardFeatures.Skull.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("bone_tree"), GraveyardFeatures.BoneTree.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("drake_nest"), GraveyardFeatures.DrakeNest.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("dead_tree"), hu.endrealms.worldgen.SculkFeatures.DeadTree.CODEC);
		Registry.register(BuiltInRegistries.BIOME_SOURCE, EndRealms.id("sculk"), hu.endrealms.sculk.SculkBiomeSource.CODEC);
		hu.endrealms.worldgen.SculkLands.register();
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("watcher_grass_patch"), hu.endrealms.worldgen.SculkFeatures.PlantPatch.WATCHER_GRASS_CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("echo_flower_patch"), hu.endrealms.worldgen.SculkFeatures.PlantPatch.ECHO_FLOWER_CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("soul_mushroom_patch"), hu.endrealms.worldgen.SculkFeatures.PlantPatch.SOUL_MUSHROOM_CODEC);

		BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd(),
				GenerationStep.Decoration.UNDERGROUND_ORES, ORE_CHORUSITE);

		BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd(), GenerationStep.Decoration.UNDERGROUND_ORES, PHASITE_VEIN);

		BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd().and(BiomeSelectors.excludeByKey(ASTRAL_FOREST, PRISM_WASTES, SCORCHED_BARRENS, DRAGON_GRAVEYARD)),
				GenerationStep.Decoration.LOCAL_MODIFICATIONS, END_SOIL_PATCH);

		BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd().and(BiomeSelectors.excludeByKey(ASTRAL_FOREST, PRISM_WASTES, SCORCHED_BARRENS, DRAGON_GRAVEYARD)),
				GenerationStep.Decoration.VEGETAL_DECORATION, PATCH_VOIDBLOOM);

		// vanilla end_highlands has weight 1.0, so ~40% of big-island centres become Astral Forest
		TheEndBiomes.addHighlandsBiome(ASTRAL_FOREST, 0.7);
		TheEndBiomes.addMidlandsBiome(ASTRAL_FOREST, ASTRAL_FOREST, 1.0);
		TheEndBiomes.addBarrensBiome(ASTRAL_FOREST, Biomes.END_BARRENS, 1.0);

		TheEndBiomes.addHighlandsBiome(PRISM_WASTES, 1.3);
		TheEndBiomes.addMidlandsBiome(PRISM_WASTES, PRISM_WASTES, 1.0);
		TheEndBiomes.addBarrensBiome(PRISM_WASTES, Biomes.END_BARRENS, 1.0);

		// Registered only so the biome is part of the End biome source; the tiny weight means the noise
		// practically never picks it - TheEndBiomeSourceMixin places it on the basin islands instead.
		TheEndBiomes.addHighlandsBiome(ASTRAL_BASIN, 1.0E-4);
		TheEndBiomes.addHighlandsBiome(SCORCHED_BARRENS, 1.0E-4);
		TheEndBiomes.addHighlandsBiome(DRAGON_GRAVEYARD, 1.0E-4);
	}
}
