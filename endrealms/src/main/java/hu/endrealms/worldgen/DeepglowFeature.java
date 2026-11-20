package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Small glowing clusters of Deepglow Ore set into the void sand of the lake bed, only well below the surface. */
public final class DeepglowFeature implements Feature {
	public static final DeepglowFeature INSTANCE = new DeepglowFeature();
	public static final MapCodec<DeepglowFeature> CODEC = MapCodec.unit(INSTANCE);
	private static final int MIN_WATER_ABOVE = 12;

	private DeepglowFeature() {
	}

	@Override
	public MapCodec<? extends Feature> codec() {
		return CODEC;
	}

	private static boolean bed(WorldGenLevel level, BlockPos surface) {
		return surface.getY() <= AstralBasinFeature.WATER_LEVEL - MIN_WATER_ABOVE && level.getBlockState(surface.below()).is(ModBlocks.VOID_SAND)
				&& !level.getFluidState(surface).isEmpty();
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!bed(level, origin)) {
			return false;
		}
		int radius = 1 + random.nextInt(2);
		boolean placed = false;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius + 1 || random.nextFloat() < 0.3F) {
					continue;
				}
				BlockPos surface = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, origin.offset(dx, 0, dz));
				if (bed(level, surface)) {
					level.setBlock(surface.below(), ModBlocks.DEEPGLOW_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
					placed = true;
				}
			}
		}
		return placed;
	}
}
