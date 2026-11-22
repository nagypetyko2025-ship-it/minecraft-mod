package hu.endrealms.sculk;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

/**
 * The Sculk dimension's biomes laid out exactly like the Overworld's, each vanilla biome standing in for one of four:
 * wherever the Overworld would raise its jagged, frozen or stony peaks (and their slopes), the Broken Peaks; its
 * forests, jungles and swamps, the Withered Woods; its badlands, deserts and savannas, the Riven Lands with their
 * canyons; everything else, the Silent Barrens. Since the terrain is the Overworld's too, every biome lands on the
 * ground that suits it - the peaks really are the mountains.
 */
public final class SculkBiomeSource extends BiomeSource {
	public static final MapCodec<SculkBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Biome.CODEC.fieldOf("barrens").forGetter(source -> source.barrens),
			Biome.CODEC.fieldOf("woods").forGetter(source -> source.woods),
			Biome.CODEC.fieldOf("peaks").forGetter(source -> source.peaks),
			Biome.CODEC.fieldOf("canyons").forGetter(source -> source.canyons),
			Biome.CODEC.fieldOf("hanging").forGetter(source -> source.hanging),
			Biome.CODEC.fieldOf("city").forGetter(source -> source.city),
			Biome.CODEC.fieldOf("bells").forGetter(source -> source.bells),
			Biome.CODEC.fieldOf("pillars").forGetter(source -> source.pillars),
			Biome.CODEC.fieldOf("ruins").forGetter(source -> source.ruins)
	).apply(instance, SculkBiomeSource::new));

	private static final Set<String> PEAKS = Set.of("jagged_peaks", "frozen_peaks", "stony_peaks", "snowy_slopes", "grove",
			"windswept_hills", "windswept_gravelly_hills");
	private static final Set<String> WOODS = Set.of("forest", "flower_forest", "birch_forest", "old_growth_birch_forest",
			"taiga", "snowy_taiga", "jungle", "sparse_jungle", "bamboo_jungle", "windswept_forest", "swamp", "mangrove_swamp");
	private static final Set<String> CANYONS = Set.of("badlands", "eroded_badlands", "wooded_badlands", "savanna",
			"savanna_plateau", "windswept_savanna");
	/** The dark, old forests: the Hanging Woods. */
	private static final Set<String> HANGING = Set.of("dark_forest", "pale_garden", "old_growth_pine_taiga", "old_growth_spruce_taiga");
	/** Gentle open land: the Shadow City. */
	private static final Set<String> CITY = Set.of("meadow", "sunflower_plains", "cherry_grove");
	/** The rivers, dry here: long winding valleys, the Bell Valley. */
	private static final Set<String> BELLS = Set.of("river", "frozen_river");
	/** Flat open land: the Listening Pillars. */
	private static final Set<String> PILLARS = Set.of("desert", "snowy_plains", "ice_spikes");
	/** The plains: the Scattered Ruins. */
	private static final Set<String> RUINS = Set.of("plains");

	private final Holder<Biome> barrens;
	private final Holder<Biome> woods;
	private final Holder<Biome> peaks;
	private final Holder<Biome> canyons;
	private final Holder<Biome> hanging;
	private final Holder<Biome> city;
	private final Holder<Biome> bells;
	private final Holder<Biome> pillars;
	private final Holder<Biome> ruins;
	private final Climate.ParameterList<Holder<Biome>> parameters;

	public SculkBiomeSource(Holder<Biome> barrens, Holder<Biome> woods, Holder<Biome> peaks, Holder<Biome> canyons,
			Holder<Biome> hanging, Holder<Biome> city, Holder<Biome> bells, Holder<Biome> pillars, Holder<Biome> ruins) {
		this.hanging = hanging;
		this.city = city;
		this.bells = bells;
		this.pillars = pillars;
		this.ruins = ruins;
		this.barrens = barrens;
		this.woods = woods;
		this.peaks = peaks;
		this.canyons = canyons;
		Climate.ParameterList<ResourceKey<Biome>> overworld = MultiNoiseBiomeSourceParameterList.knownPresets()
				.get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD);
		List<Pair<Climate.ParameterPoint, Holder<Biome>>> points = new ArrayList<>();
		for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> point : overworld.values()) {
			points.add(Pair.of(point.getFirst(), standIn(point.getSecond())));
		}
		this.parameters = new Climate.ParameterList<>(points);
	}

	private Holder<Biome> standIn(ResourceKey<Biome> vanilla) {
		String name = vanilla.identifier().getPath();
		return PEAKS.contains(name) ? peaks : WOODS.contains(name) ? woods : CANYONS.contains(name) ? canyons
				: HANGING.contains(name) ? hanging : CITY.contains(name) ? city : BELLS.contains(name) ? bells
				: PILLARS.contains(name) ? pillars : RUINS.contains(name) ? ruins : barrens;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return Stream.of(barrens, woods, peaks, canyons, hanging, city, bells, pillars, ruins);
	}

	@Override
	public BiomeResolver createResolver(Climate.Sampler sampler) {
		return (x, y, z) -> parameters.findValue(sampler.sample(x, y, z));
	}
}
