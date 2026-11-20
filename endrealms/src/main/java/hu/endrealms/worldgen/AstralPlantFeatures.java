package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.block.MoonbellBlock;
import hu.endrealms.block.StarreedBlock;
import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** The tall plants of the Astral Forest, listed only in that biome's own feature list. */
public final class AstralPlantFeatures {
	private AstralPlantFeatures() {
	}

	/** Random ground position around origin with free end soil under it and enough air above, or null. */
	private static BlockPos spot(WorldGenLevel level, RandomSource random, BlockPos origin, int spread, int height) {
		BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				origin.offset(random.nextInt(spread * 2 + 1) - spread, 0, random.nextInt(spread * 2 + 1) - spread));
		if (!level.getBlockState(pos.below()).is(ModBlocks.END_SOIL)) {
			return null;
		}
		for (int h = 0; h < height; h++) {
			if (!level.getBlockState(pos.above(h)).isAir()) {
				return null;
			}
		}
		return pos;
	}

	/** A dense reed bed. */
	public static final class Starreed implements Feature {
		public static final Starreed INSTANCE = new Starreed();
		public static final MapCodec<Starreed> CODEC = MapCodec.unit(INSTANCE);

		private Starreed() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			boolean placed = false;
			BlockState reed = ModBlocks.STARREED.defaultBlockState();
			for (int i = 0; i < 28; i++) {
				int height = 3 + random.nextInt(StarreedBlock.MAX_HEIGHT - 2);
				BlockPos pos = spot(level, random, origin, 4, height);
				if (pos != null) {
					for (int h = 0; h < height; h++) {
						level.setBlock(pos.above(h), reed.setValue(StarreedBlock.TOP, h == height - 1), Block.UPDATE_CLIENTS);
					}
					placed = true;
				}
			}
			return placed;
		}
	}

	/** A few moonbells close together. */
	public static final class Moonbell implements Feature {
		public static final Moonbell INSTANCE = new Moonbell();
		public static final MapCodec<Moonbell> CODEC = MapCodec.unit(INSTANCE);

		private Moonbell() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			boolean placed = false;
			for (int i = 0; i < 6; i++) {
				BlockPos pos = spot(level, random, origin, 3, 3);
				if (pos != null) {
					MoonbellBlock.placeWhole(level, pos, Block.UPDATE_CLIENTS);
					placed = true;
				}
			}
			return placed;
		}
	}
}
