package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModFluids;
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
 * Builds an Astral Basin island (see {@link BasinSites}): a big island hanging into the void, crowned by a ring
 * of mountains, with a deep crater lake of astral water filled right up to the inner rim. The lake bed and the
 * narrow inner beach are void sand. Runs once per chunk and edits only that chunk's own columns; every column's
 * shape depends only on its position, so the island lines up seamlessly across chunks.
 */
public final class AstralBasinFeature implements Feature {
	public static final AstralBasinFeature INSTANCE = new AstralBasinFeature();
	public static final MapCodec<AstralBasinFeature> CODEC = MapCodec.unit(INSTANCE);

	/** Lake surface. The lowest point of the rim is always a few blocks above it, so the lake is full. */
	public static final int WATER_LEVEL = 88;

	private AstralBasinFeature() {
	}

	@Override
	public MapCodec<? extends Feature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		BasinSites.Site site = BasinSites.siteFor(chunkX * 16 + 8, chunkZ * 16 + 8);
		if (site == null) {
			return false;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockState endStone = Blocks.END_STONE.defaultBlockState();
		BlockState voidstone = ModBlocks.VOIDSTONE.defaultBlockState();
		BlockState sand = ModBlocks.VOID_SAND.defaultBlockState();
		BlockState water = ModFluids.ASTRAL_WATER_BLOCK.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		boolean placed = false;

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = chunkX * 16 + dx;
				int z = chunkZ * 16 + dz;
				float n = BasinSites.noise(x, z, 16, 7);       // outline wobble
				float crestNoise = BasinSites.noise(x, z, 28, 11);  // peaks and saddles along the ring
				float rough = BasinSites.noise(x, z, 6, 13);   // small bumps
				float r = Mth.sqrt(Mth.square(x - site.x()) + Mth.square(z - site.z()));
				float ringR = site.ringRadius(n);
				if (r > ringR) {
					continue;
				}
				placed = true;
				float lakeR = site.lakeRadius(n);
				int existingTop = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				boolean lake = r <= lakeR;

				int surface;
				if (lake) {
					// bowl: deepest in the middle (up to 38 blocks of water)
					surface = WATER_LEVEL - Math.min(38, 5 + (int) ((lakeR - r) * 1.0F + 3.0F * n));
				} else {
					float t = (r - lakeR) / (ringR - lakeR);
					int crest = WATER_LEVEL + 6 + (int) (24.0F * (0.5F + 0.5F * crestNoise) + 5.0F * n);
					int outer = WATER_LEVEL - 24 + (int) (4.0F * n);
					float h;
					if (t < 0.3F) {          // steep inner wall rising out of the lake
						h = WATER_LEVEL + 2 + (crest - WATER_LEVEL - 2) * smooth(t / 0.3F);
					} else {                  // long outer slope down to the island rim
						h = crest + (outer - crest) * smooth((t - 0.3F) / 0.7F);
					}
					surface = (int) (h + 2.5F * rough * Math.min(1.0F, t * 4.0F));
				}

				// hanging underside: deepest under the middle, like a natural End island
				float edge = 1.0F - Mth.square(r / ringR);
				int bottom = Math.min(surface - 6, 34 - (int) (30.0F * Mth.sqrt(Math.max(0.0F, edge)) + 4.0F * n));
				boolean beach = !lake && surface <= WATER_LEVEL + 3;

				for (int y = bottom; y <= surface; y++) {
					int depth = surface - y;
					BlockState state;
					if ((lake || beach) && depth < 3) {
						state = sand;               // lake bed and inner beach
					} else {
						state = depth < 3 ? endStone : voidstone;
					}
					level.setBlock(pos.set(x, y, z), state, 2);
				}
				if (lake) {
					for (int y = surface + 1; y <= WATER_LEVEL; y++) {
						level.setBlock(pos.set(x, y, z), water, 2);
					}
				}
				int clearFrom = (lake ? WATER_LEVEL : surface) + 1;
				for (int y = clearFrom; y <= existingTop; y++) {
					level.setBlock(pos.set(x, y, z), air, 2);
				}
			}
		}
		return placed;
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
