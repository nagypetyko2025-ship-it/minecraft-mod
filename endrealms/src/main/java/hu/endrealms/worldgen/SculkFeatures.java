package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Features of the Sculk dimension. */
public final class SculkFeatures {
	private static final int FLAGS = 2;

	private SculkFeatures() {
	}

	/**
	 * A dead tree: a crooked bare trunk of dark wood that leans as it grows, with a few thin dead branches and no leaf
	 * anywhere; now and then sculk has crept up its foot. In the Withered Woods they stand close together, on the
	 * Silent Barrens alone.
	 */
	public static final class DeadTree implements Feature {
		public static final DeadTree INSTANCE = new DeadTree();
		public static final MapCodec<DeadTree> CODEC = MapCodec.unit(INSTANCE);

		private DeadTree() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			BlockState ground = level.getBlockState(origin.below());
			if (ground.getCollisionShape(level, origin.below()).isEmpty() || !ground.getFluidState().isEmpty() || !level.getBlockState(origin).isAir()) {
				return false;
			}
			boolean pale = random.nextInt(4) == 0;
			int height = 5 + random.nextInt(6);
			float lean = random.nextFloat() * Mth.TWO_PI;
			float leanStrength = 0.15F + random.nextFloat() * 0.25F;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int x = 0;
			int z = 0;
			for (int y = 0; y < height; y++) {
				if (y > 2) {
					x = Math.round(Mth.cos(lean) * leanStrength * (y - 2));
					z = Math.round(Mth.sin(lean) * leanStrength * (y - 2));
				}
				set(level, pos.setWithOffset(origin, x, y, z), log(pale, Direction.Axis.Y));
			}
			if (random.nextInt(3) == 0) {                                   // sculk creeping up the foot
				level.setBlock(origin.below(), Blocks.SCULK.defaultBlockState(), FLAGS);
			}
			int branches = 2 + random.nextInt(3);
			for (int i = 0; i < branches; i++) {
				float angle = random.nextFloat() * Mth.TWO_PI;
				int baseY = height / 2 + random.nextInt(Math.max(1, height / 2));
				int length = 2 + random.nextInt(3);
				int bx = Math.round(Mth.cos(lean) * leanStrength * Math.max(0, baseY - 2));
				int bz = Math.round(Mth.sin(lean) * leanStrength * Math.max(0, baseY - 2));
				Direction.Axis axis = Math.abs(Mth.cos(angle)) > Math.abs(Mth.sin(angle)) ? Direction.Axis.X : Direction.Axis.Z;
				for (int s = 1; s <= length; s++) {
					int dx = bx + Math.round(Mth.cos(angle) * s);
					int dz = bz + Math.round(Mth.sin(angle) * s);
					int dy = baseY + (s >= 2 ? 1 : 0) + (s >= 4 ? 1 : 0);
					set(level, pos.setWithOffset(origin, dx, dy, dz), log(pale, axis));
				}
			}
			return true;
		}

		private static BlockState log(boolean pale, Direction.Axis axis) {
			return (pale ? Blocks.STRIPPED_PALE_OAK_LOG : Blocks.STRIPPED_DARK_OAK_LOG).defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
		}

		private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
			if (level.getBlockState(pos).canBeReplaced() || level.getBlockState(pos).isAir()) {
				level.setBlock(pos, state, FLAGS);
			}
		}
	}

	/**
	 * A patch of plants around the origin: a number of tries scattered within a radius, each standing on the ground
	 * of the dimension. Watcher Grass grows thick, three blocks tall; the others are single blocks.
	 */
	public static final class PlantPatch implements Feature {
		public static final PlantPatch WATCHER_GRASS = new PlantPatch(56, 6, null);
		public static final PlantPatch ECHO_FLOWER = new PlantPatch(10, 4, () -> hu.endrealms.registry.ModBlocks.ECHO_FLOWER.defaultBlockState());
		public static final PlantPatch SOUL_MUSHROOM = new PlantPatch(8, 3, () -> hu.endrealms.registry.ModBlocks.SOUL_MUSHROOM.defaultBlockState());
		public static final MapCodec<PlantPatch> WATCHER_GRASS_CODEC = MapCodec.unit(WATCHER_GRASS);
		public static final MapCodec<PlantPatch> ECHO_FLOWER_CODEC = MapCodec.unit(ECHO_FLOWER);
		public static final MapCodec<PlantPatch> SOUL_MUSHROOM_CODEC = MapCodec.unit(SOUL_MUSHROOM);

		private final int tries;
		private final int radius;
		private final java.util.function.Supplier<BlockState> plant;

		private PlantPatch(int tries, int radius, java.util.function.Supplier<BlockState> plant) {
			this.tries = tries;
			this.radius = radius;
			this.plant = plant;
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return this == WATCHER_GRASS ? WATCHER_GRASS_CODEC : this == ECHO_FLOWER ? ECHO_FLOWER_CODEC : SOUL_MUSHROOM_CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			int placed = 0;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int i = 0; i < tries; i++) {
				pos.setWithOffset(origin, random.nextInt(radius * 2 + 1) - radius, random.nextInt(7) - 3, random.nextInt(radius * 2 + 1) - radius);
				if (plant == null) {
					if (hu.endrealms.sculk.SculkPlants.WatcherGrass.place(level, pos, random, FLAGS)) {
						placed++;
					}
				} else if (level.isEmptyBlock(pos) && hu.endrealms.sculk.SculkPlants.ground(level.getBlockState(pos.below()))) {
					level.setBlock(pos, plant.get(), FLAGS);
					placed++;
				}
			}
			return placed > 0;
		}
	}
}
