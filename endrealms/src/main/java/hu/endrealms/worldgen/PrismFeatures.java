package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** The two Prism Wastes decorations; both only start on crystalline voidstone, so they stay inside the biome's ground. */
public final class PrismFeatures {
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	private PrismFeatures() {
	}

	private static boolean onGround(WorldGenLevel level, BlockPos surface) {
		return level.getBlockState(surface.below()).is(ModBlocks.CRYSTALLINE_VOIDSTONE);
	}

	private static BlockState randomBud(RandomSource random, Direction facing) {
		int roll = random.nextInt(10);
		Block block = roll < 3 ? ModBlocks.SMALL_PRISM_BUD : roll < 5 ? ModBlocks.MEDIUM_PRISM_BUD
				: roll < 7 ? ModBlocks.LARGE_PRISM_BUD : ModBlocks.PRISM_CLUSTER;
		return block.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
	}

	private static BlockState crystal(RandomSource random) {
		return (random.nextFloat() < 0.07F ? ModBlocks.BUDDING_PRISM_CRYSTAL : ModBlocks.PRISM_CRYSTAL_BLOCK).defaultBlockState();
	}

	/** A sheaf of leaning crystal needles: one tall main spire ringed by a few shorter ones, buds on their faces. */
	public static final class Spire implements Feature {
		public static final Spire INSTANCE = new Spire();
		public static final MapCodec<Spire> CODEC = MapCodec.unit(INSTANCE);

		private Spire() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			if (!onGround(level, origin)) {
				return false;
			}
			needle(level, random, origin, 14 + random.nextInt(21), 2.2F + random.nextFloat() * 1.6F);
			int satellites = 2 + random.nextInt(4);
			for (int i = 0; i < satellites; i++) {
				float angle = random.nextFloat() * Mth.TWO_PI;
				float distance = 4.0F + random.nextFloat() * 3.5F;
				BlockPos surface = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR,
						origin.offset(Mth.floor(Mth.cos(angle) * distance), 0, Mth.floor(Mth.sin(angle) * distance)));
				if (onGround(level, surface)) {
					needle(level, random, surface, 5 + random.nextInt(9), 1.0F + random.nextFloat() * 0.9F);
				}
			}
			return true;
		}

		private static void needle(WorldGenLevel level, RandomSource random, BlockPos base, int height, float radius) {
			float leanX = (random.nextFloat() - 0.5F) * 0.45F;
			float leanZ = (random.nextFloat() - 0.5F) * 0.45F;
			List<BlockPos> faces = new ArrayList<>();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int y = -2; y <= height; y++) {
				float t = Math.max(0.0F, y) / (float) height;
				float r = radius * (float) Math.pow(1.0F - t, 0.75F) + 0.3F;
				int reach = Mth.ceil(r);
				int centerX = Math.round(leanX * Math.max(0, y));
				int centerZ = Math.round(leanZ * Math.max(0, y));
				for (int dx = -reach; dx <= reach; dx++) {
					for (int dz = -reach; dz <= reach; dz++) {
						if (dx * dx + dz * dz > r * r) {
							continue;
						}
						pos.set(base.getX() + centerX + dx, base.getY() + y, base.getZ() + centerZ + dz);
						if (y >= 0 && !level.getBlockState(pos).isAir()) {
							continue;
						}
						level.setBlock(pos, y >= 0 ? crystal(random) : ModBlocks.PRISM_CRYSTAL_BLOCK.defaultBlockState(), FLAGS);
						if (y >= 0) {
							faces.add(pos.immutable());
						}
					}
				}
			}
			// only after the whole needle stands, so that "free side" really means free
			for (BlockPos face : faces) {
				for (Direction direction : Direction.values()) {
					BlockPos side = face.relative(direction);
					if (random.nextFloat() < 0.07F && level.getBlockState(side).isAir()) {
						level.setBlock(side, randomBud(random, direction), FLAGS);
					}
				}
			}
		}
	}

	/** A small crystal outcrop set flush into the ground, with buds growing out of it. */
	public static final class Patch implements Feature {
		public static final Patch INSTANCE = new Patch();
		public static final MapCodec<Patch> CODEC = MapCodec.unit(INSTANCE);

		private Patch() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			if (!onGround(level, origin)) {
				return false;
			}
			int radius = 1 + random.nextInt(3);
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx * dx + dz * dz > radius * radius + 1 || random.nextFloat() < 0.2F) {
						continue;
					}
					BlockPos surface = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, origin.offset(dx, 0, dz));
					if (!onGround(level, surface)) {
						continue;
					}
					level.setBlock(surface.below(), crystal(random), FLAGS);
					if (random.nextFloat() < 0.6F && level.getBlockState(surface).isAir()) {
						level.setBlock(surface, randomBud(random, Direction.UP), FLAGS);
					}
				}
			}
			return true;
		}
	}
}
