package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModBlocks;
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
 * Builds a Scorched Barrens island (see {@link Sites}): rolling hills and valleys that sink to a low rim, over a
 * hanging underside. The biome covers exactly this island, so the ash and volcanoes fill it from edge to edge. Runs
 * once per chunk and edits only that chunk's columns; each column depends only on its position, so chunks line up.
 */
public final class ScorchedIslandFeature implements Feature {
	public static final ScorchedIslandFeature INSTANCE = new ScorchedIslandFeature();
	public static final MapCodec<ScorchedIslandFeature> CODEC = MapCodec.unit(INSTANCE);

	private static final int RIM_LEVEL = 56;
	private static final int MEAN_LEVEL = 74;

	/** Where the islands are. Uses the Astral Basin's grid and only takes cells without a basin, so the two never overlap. */
	public static final class Sites {
		private static final int CELL = 640;
		private static final int CELL_MARGIN = 128;
		private static final float CHANCE = 0.75F;
		private static final double MIN_DIST_FROM_CENTER = 1100.0;

		public record Site(int x, int z, float radius) {
		}

		private Sites() {
		}

		public static Site siteFor(int x, int z) {
			int cx = Math.floorDiv(x, CELL);
			int cz = Math.floorDiv(z, CELL);
			if (BasinSites.hash(cx, cz, 41) >= CHANCE || BasinSites.siteFor(x, z) != null) {
				return null;
			}
			int span = CELL - 2 * CELL_MARGIN;
			int sx = cx * CELL + CELL_MARGIN + (int) (BasinSites.hash(cx, cz, 42) * span);
			int sz = cz * CELL + CELL_MARGIN + (int) (BasinSites.hash(cx, cz, 43) * span);
			if ((double) sx * sx + (double) sz * sz < MIN_DIST_FROM_CENTER * MIN_DIST_FROM_CENTER) {
				return null;
			}
			return new Site(sx, sz, 64.0F + 26.0F * BasinSites.hash(cx, cz, 44));
		}

		/** True if the biome at this column should be Scorched Barrens (the island plus a small margin). */
		public static boolean isScorchedBiome(int x, int z) {
			Site site = siteFor(x, z);
			if (site == null) {
				return false;
			}
			return Mth.square(x - site.x()) + Mth.square(z - site.z()) <= Mth.square(site.radius() + 12.0F);
		}
	}

	private ScorchedIslandFeature() {
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
		BlockState scorched = ModBlocks.SCORCHED_END_STONE.defaultBlockState();
		BlockState voidstone = ModBlocks.VOIDSTONE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		boolean placed = false;
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = chunkX * 16 + dx;
				int z = chunkZ * 16 + dz;
				float wobble = BasinSites.noise(x, z, 16, 45);
				float radius = site.radius() + 7.0F * wobble;
				float r = Mth.sqrt(Mth.square(x - site.x()) + Mth.square(z - site.z()));
				if (r > radius) {
					continue;
				}
				placed = true;
				int surface = surfaceHeight(x, z, radius, r);
				float depthShape = Mth.sqrt(Math.max(0.0F, 1.0F - Mth.square(r / radius)));
				int bottom = Math.min(surface - 6, 34 - (int) (30.0F * depthShape + 4.0F * wobble));
				int existingTop = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				for (int y = bottom; y <= surface; y++) {
					level.setBlock(pos.set(x, y, z), surface - y < 3 ? scorched : voidstone, 2);
				}
				for (int y = surface + 1; y <= existingTop; y++) {
					level.setBlock(pos.set(x, y, z), air, 2);
				}
			}
		}
		return placed;
	}

	private static int surfaceHeight(int x, int z, float radius, float r) {
		float hills = 13.0F * BasinSites.noise(x, z, 46, 46) + 8.0F * BasinSites.noise(x, z, 21, 47) + 2.5F * BasinSites.noise(x, z, 8, 48);
		float edge = smooth((radius - r) / (radius * 0.28F));
		return (int) (RIM_LEVEL + (MEAN_LEVEL + hills - RIM_LEVEL) * edge);
	}

	/** The island's ground height at a column inside it. Structures need this: the terrain generator knows nothing of these islands. */
	public static int surfaceAt(Sites.Site site, int x, int z) {
		float radius = site.radius() + 7.0F * BasinSites.noise(x, z, 16, 45);
		return surfaceHeight(x, z, radius, Mth.sqrt(Mth.square(x - site.x()) + Mth.square(z - site.z())));
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
