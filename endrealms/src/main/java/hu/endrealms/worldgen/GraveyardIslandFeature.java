package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Builds a Dragon Graveyard island (see {@link Sites}): low rolling ground over a hanging underside, covered by the
 * biome from edge to edge. Each column depends only on its position, so chunks line up.
 */
public final class GraveyardIslandFeature implements Feature {
	public static final GraveyardIslandFeature INSTANCE = new GraveyardIslandFeature();
	public static final MapCodec<GraveyardIslandFeature> CODEC = MapCodec.unit(INSTANCE);

	private static final int RIM_LEVEL = 56;
	private static final int MEAN_LEVEL = 66;

	/** Where the islands are: a grid of their own, skipping anywhere an Astral Basin or Scorched island would overlap. */
	public static final class Sites {
		public static final int CELL = 800;
		private static final int CELL_MARGIN = 140;
		private static final float CHANCE = 0.65F;
		private static final double MIN_DIST_FROM_CENTER = 1100.0;
		private static final int OTHER_GRID = 640;
		private static final float CLEARANCE = 40.0F;
		private static final Map<Long, Optional<Site>> CACHE = new ConcurrentHashMap<>();

		public record Site(int x, int z, float radius) {
		}

		private Sites() {
		}

		public static Site siteFor(int x, int z) {
			int cx = Math.floorDiv(x, CELL);
			int cz = Math.floorDiv(z, CELL);
			return CACHE.computeIfAbsent(((long) cx << 32) ^ (cz & 0xFFFFFFFFL), key -> Optional.ofNullable(compute(cx, cz))).orElse(null);
		}

		private static Site compute(int cx, int cz) {
			if (BasinSites.hash(cx, cz, 91) >= CHANCE) {
				return null;
			}
			int span = CELL - 2 * CELL_MARGIN;
			int sx = cx * CELL + CELL_MARGIN + (int) (BasinSites.hash(cx, cz, 92) * span);
			int sz = cz * CELL + CELL_MARGIN + (int) (BasinSites.hash(cx, cz, 93) * span);
			if ((double) sx * sx + (double) sz * sz < MIN_DIST_FROM_CENTER * MIN_DIST_FROM_CENTER) {
				return null;
			}
			float radius = 62.0F + 22.0F * BasinSites.hash(cx, cz, 94);
			for (int gx = -1; gx <= 1; gx++) {
				for (int gz = -1; gz <= 1; gz++) {
					int px = sx + gx * OTHER_GRID;
					int pz = sz + gz * OTHER_GRID;
					BasinSites.Site basin = BasinSites.siteFor(px, pz);
					if (basin != null && overlaps(sx, sz, radius, basin.x(), basin.z(), basin.radius())) {
						return null;
					}
					ScorchedIslandFeature.Sites.Site scorched = ScorchedIslandFeature.Sites.siteFor(px, pz);
					if (scorched != null && overlaps(sx, sz, radius, scorched.x(), scorched.z(), scorched.radius())) {
						return null;
					}
				}
			}
			return new Site(sx, sz, radius);
		}

		private static boolean overlaps(int x, int z, float radius, int otherX, int otherZ, float otherRadius) {
			return Mth.square((float) (x - otherX)) + Mth.square((float) (z - otherZ)) < Mth.square(radius + otherRadius + CLEARANCE);
		}

		/** True if the biome at this column should be Dragon Graveyard (the island plus a small margin). */
		public static boolean isGraveyardBiome(int x, int z) {
			Site site = siteFor(x, z);
			return site != null && Mth.square((float) (x - site.x())) + Mth.square((float) (z - site.z())) <= Mth.square(site.radius() + 12.0F);
		}
	}

	private GraveyardIslandFeature() {
	}

	@Override
	public MapCodec<? extends Feature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		Sites.Site site = Sites.siteFor(chunkX * 16 + 8, chunkZ * 16 + 8);
		if (site == null) {
			return false;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockState stone = Blocks.END_STONE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		boolean placed = false;
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = chunkX * 16 + dx;
				int z = chunkZ * 16 + dz;
				float wobble = BasinSites.noise(x, z, 16, 95);
				float radius = site.radius() + 6.0F * wobble;
				float r = Mth.sqrt(Mth.square((float) (x - site.x())) + Mth.square((float) (z - site.z())));
				if (r > radius) {
					continue;
				}
				placed = true;
				float hills = 4.0F * BasinSites.noise(x, z, 34, 96) + 1.5F * BasinSites.noise(x, z, 12, 97);
				float t = Mth.clamp((radius - r) / (radius * 0.3F), 0.0F, 1.0F);
				int surface = (int) (RIM_LEVEL + (MEAN_LEVEL + hills - RIM_LEVEL) * t * t * (3.0F - 2.0F * t));
				float depth = Mth.sqrt(Math.max(0.0F, 1.0F - Mth.square(r / radius)));
				int bottom = Math.min(surface - 6, 36 - (int) (28.0F * depth + 4.0F * wobble));
				int existingTop = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				for (int y = bottom; y <= surface; y++) {
					level.setBlock(pos.set(x, y, z), stone, 2);
				}
				for (int y = surface + 1; y <= existingTop; y++) {
					level.setBlock(pos.set(x, y, z), air, 2);
				}
			}
		}
		return placed;
	}
}
