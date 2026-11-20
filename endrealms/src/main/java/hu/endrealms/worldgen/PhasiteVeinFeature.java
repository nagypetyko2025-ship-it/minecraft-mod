package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** A small phasite vein on the underside of an End island: climbs up from the lowest block of the column. */
public final class PhasiteVeinFeature implements Feature {
	public static final PhasiteVeinFeature INSTANCE = new PhasiteVeinFeature();
	public static final MapCodec<PhasiteVeinFeature> CODEC = MapCodec.unit(INSTANCE);
	private static final int VEIN_HEIGHT = 3;

	private PhasiteVeinFeature() {
	}

	@Override
	public MapCodec<? extends Feature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
		if (top <= level.getMinY()) {
			return false;   // nothing but void in this column
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(origin.getX(), level.getMinY(), origin.getZ());
		while (pos.getY() < top && level.getBlockState(pos).isAir()) {
			pos.move(0, 1, 0);
		}
		BlockState ore = ModBlocks.PHASITE_ORE.defaultBlockState();
		int bottom = pos.getY();
		int radius = 1 + random.nextInt(2);
		boolean placed = false;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dy = 0; dy < VEIN_HEIGHT; dy++) {
					pos.set(origin.getX() + dx, bottom + dy, origin.getZ() + dz);
					BlockState state = level.getBlockState(pos);
					if ((state.is(Blocks.END_STONE) || state.is(ModBlocks.VOIDSTONE)) && random.nextFloat() < 0.55F) {
						level.setBlock(pos, ore, 2);
						placed = true;
					}
				}
			}
		}
		return placed;
	}
}
