package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.sculk.SculkWilds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The stranger lands of the Sculk dimension: the Hanging Woods, the Shadow City, the Bell Valley, the Listening Pillars
 * and the Scattered Ruins. Each piece fits in the 3x3 chunks a feature may touch around its own chunk.
 */
public final class SculkLands {
	private static final int FLAGS = 2;
	static final ResourceKey<LootTable> RUIN = loot("sculk_ruin");
	static final ResourceKey<LootTable> CITY = loot("shadow_city");
	static final ResourceKey<LootTable> HERMIT = loot("hermit_pillar");
	static final ResourceKey<LootTable> NEST = loot("spire_nest");

	private SculkLands() {
	}

	private static ResourceKey<LootTable> loot(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/" + name));
	}

	static BlockState state(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)).defaultBlockState();
	}

	static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, FLAGS);
	}

	/** Places only into air or things that give way (plants, veins). */
	static void soft(WorldGenLevel level, BlockPos pos, BlockState state) {
		BlockState there = level.getBlockState(pos);
		if (there.isAir() || there.canBeReplaced()) {
			level.setBlock(pos, state, FLAGS);
		}
	}

	static void chest(WorldGenLevel level, RandomSource random, BlockPos pos, ResourceKey<LootTable> table) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), FLAGS);
		RandomizableContainer.setBlockEntityLootTable(level, random, pos, table);
	}

	static <T extends Mob> void spawn(WorldGenLevel level, EntityType<T> type, BlockPos pos, RandomSource random) {
		T mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob != null) {
			mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			mob.setPersistenceRequired();
			// no finalizeSpawn here: it asks the real level for the difficulty, which from a world generation thread
			// waits on the server thread while that waits on this chunk - a deadlock
			if (mob instanceof hu.endrealms.entity.Dangler dangler) {
				dangler.hangFromAbove(level);
			}
			level.addFreshEntityWithPassengers(mob);
		}
	}

	static boolean solidGround(WorldGenLevel level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return !below.getCollisionShape(level, pos.below()).isEmpty() && below.getFluidState().isEmpty();
	}

	private abstract static class Unit implements Feature {
		@Override
		public abstract MapCodec<? extends Feature> codec();
	}

	// ================================================================ the Hanging Woods

	/**
	 * A floating mass of sculk and soil high over the ground, its roots reaching up out of the top, and one to three dead
	 * trees growing down from its underside, their crowns of grey leaves hanging at the bottom, moss and pale berries
	 * dangling from them. Now and then a Dangler hangs among the leaves.
	 */
	public static final class HangingIsland extends Unit {
		public static final HangingIsland INSTANCE = new HangingIsland();
		public static final MapCodec<HangingIsland> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			BlockPos center = new BlockPos(origin.getX(), ground + 20 + random.nextInt(12), origin.getZ());
			int rx = 4 + random.nextInt(4);
			int rz = 4 + random.nextInt(4);
			int ry = 2 + random.nextInt(2);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			BlockState soil = ModBlocks.SCULK_SOIL.defaultBlockState();
			BlockState sculk = Blocks.SCULK.defaultBlockState();
			for (int dx = -rx; dx <= rx; dx++) {
				for (int dz = -rz; dz <= rz; dz++) {
					for (int dy = -ry * 2; dy <= ry; dy++) {
						double d = (dx * dx) / (double) (rx * rx) + (dz * dz) / (double) (rz * rz)
								+ (dy * dy) / (double) (dy < 0 ? ry * ry * 4 : ry * ry);
						if (d <= 1.0 - random.nextFloat() * 0.15) {
							set(level, pos.setWithOffset(center, dx, dy, dz), dy >= ry - 1 || random.nextInt(5) == 0 ? sculk : soil);
						}
					}
				}
			}
			BlockState root = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
			for (int i = 0; i < 3 + random.nextInt(4); i++) {             // roots reaching up out of the top
				int x = random.nextInt(rx + 1) - rx / 2;
				int z = random.nextInt(rz + 1) - rz / 2;
				for (int up = 1; up <= 1 + random.nextInt(4); up++) {
					soft(level, pos.setWithOffset(center, x + (up > 2 ? 1 : 0), ry + up, z), root);
				}
			}
			int trees = 1 + random.nextInt(3);
			for (int t = 0; t < trees; t++) {
				int x = random.nextInt(rx) - rx / 2;
				int z = random.nextInt(rz) - rz / 2;
				hangTree(level, random, center.offset(x, -ry * 2, z));
			}
			return true;
		}

		private static void hangTree(WorldGenLevel level, RandomSource random, BlockPos top) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int length = 4 + random.nextInt(5);
			BlockState log = (random.nextBoolean() ? Blocks.STRIPPED_DARK_OAK_LOG : Blocks.DARK_OAK_LOG).defaultBlockState();
			for (int down = 0; down < length; down++) {
				soft(level, pos.setWithOffset(top, 0, -down, 0), log);
			}
			BlockPos crown = top.below(length);
			BlockState leaves = Blocks.PALE_OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
			int r = 2 + random.nextInt(2);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					for (int dy = -r; dy <= 0; dy++) {
						if (dx * dx + dz * dz + dy * dy * 2 <= r * r + 1 && random.nextInt(8) != 0) {
							soft(level, pos.setWithOffset(crown, dx, dy, dz), leaves);
						}
					}
				}
			}
			for (int i = 0; i < 10; i++) {                               // moss and berries hanging from the crown
				int dx = random.nextInt(r * 2 + 1) - r;
				int dz = random.nextInt(r * 2 + 1) - r;
				BlockPos under = crown.offset(dx, 0, dz);
				for (int k = 0; k < r + 2 && !level.getBlockState(under).isAir(); k++) {    // down through the crown to its underside
					under = under.below();
				}
				if (level.getBlockState(under.above()).is(Blocks.PALE_OAK_LEAVES)) {
					soft(level, under, random.nextInt(3) == 0 ? ModBlocks.DANGLE_BERRY_BUSH.defaultBlockState() : Blocks.PALE_HANGING_MOSS.defaultBlockState());
				}
			}
			if (random.nextInt(3) == 0) {
				spawn(level, ModEntities.DANGLER, crown.below(r + 3), random);
			}
		}
	}

	// ================================================================ the Shadow City

	/**
	 * One building of the Shadow City per chunk: a house of Sculk Stone Bricks with Echo Glass windows, or a narrow
	 * tower, or a colonnade - always half fallen, with gaps in the walls and the roof long gone. Some keep a chest, and
	 * Shades walk among them.
	 */
	public static final class CityBuilding extends Unit {
		public static final CityBuilding INSTANCE = new CityBuilding();
		public static final MapCodec<CityBuilding> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			BlockPos base = new BlockPos(origin.getX(), y, origin.getZ());
			switch (random.nextInt(5)) {
				case 0, 1, 2 -> house(level, random, base);
				case 3 -> tower(level, random, base);
				default -> colonnade(level, random, base);
			}
			if (random.nextInt(2) == 0) {
				spawn(level, ModEntities.SHADE, base.offset(random.nextInt(5) - 2, 1, random.nextInt(5) - 2), random);
			}
			return true;
		}

		private static BlockState wall(RandomSource random) {
			int r = random.nextInt(10);
			return r < 6 ? ModBlocks.SCULK_STONE_BRICKS.defaultBlockState() : r < 8 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
					: r < 9 ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : ModBlocks.SCULK_STONE.defaultBlockState();
		}

		/** Fills down under a floor so the building stands on the ground, not over a hole. */
		static void foundation(WorldGenLevel level, BlockPos pos) {
			BlockPos.MutableBlockPos p = pos.mutable();
			for (int i = 0; i < 12 && (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()); i++) {
				set(level, p, ModBlocks.SCULK_STONE.defaultBlockState());
				p.move(Direction.DOWN);
			}
		}

		private static void house(WorldGenLevel level, RandomSource random, BlockPos base) {
			int w = 3 + random.nextInt(2);
			int d = 3 + random.nextInt(2);
			int h = 4 + random.nextInt(3);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int doorSide = random.nextInt(4);
			for (int dx = -w; dx <= w; dx++) {
				for (int dz = -d; dz <= d; dz++) {
					pos.setWithOffset(base, dx, -1, dz);
					foundation(level, pos.below());
					set(level, pos, random.nextInt(4) == 0 ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
					boolean edge = Math.abs(dx) == w || Math.abs(dz) == d;
					for (int dy = 0; dy < h + 1; dy++) {
						pos.setWithOffset(base, dx, dy, dz);
						if (!edge) {
							if (dy < 3) {
								set(level, pos, Blocks.AIR.defaultBlockState());
							}
							continue;
						}
						boolean door = dy < 2 && ((doorSide == 0 && dz == -d && dx == 0) || (doorSide == 1 && dz == d && dx == 0)
								|| (doorSide == 2 && dx == -w && dz == 0) || (doorSide == 3 && dx == w && dz == 0));
						int crumble = h - Math.abs((dx * 3 + dz * 7) % 3) - random.nextInt(3);     // the broken top edge
						if (door || dy > crumble || random.nextInt(9) == 0) {
							set(level, pos, Blocks.AIR.defaultBlockState());
						} else if (dy == 2 && (dx + dz) % 2 == 0) {
							set(level, pos, ModBlocks.ECHO_GLASS.defaultBlockState());          // a row of windows
						} else {
							set(level, pos, wall(random));
						}
					}
				}
			}
			if (random.nextInt(5) < 2) {
				chest(level, random, base.offset(w - 1, 0, d - 1), CITY);
			}
			if (random.nextBoolean()) {
				soft(level, base.offset(-w + 1, 0, -d + 1), Blocks.COBWEB.defaultBlockState());
			}
		}

		private static void tower(WorldGenLevel level, RandomSource random, BlockPos base) {
			int h = 10 + random.nextInt(8);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					pos.setWithOffset(base, dx, -1, dz);
					foundation(level, pos.below());
					set(level, pos, Blocks.DEEPSLATE_TILES.defaultBlockState());
					boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
					int top = h - random.nextInt(4);
					for (int dy = 0; dy < h; dy++) {
						pos.setWithOffset(base, dx, dy, dz);
						if (!edge || dy > top || (dy < 2 && dx == 0 && dz == -2)) {
							set(level, pos, Blocks.AIR.defaultBlockState());
						} else {
							set(level, pos, dy % 4 == 2 && (dx == 0 || dz == 0) ? ModBlocks.ECHO_GLASS.defaultBlockState() : wall(random));
						}
					}
				}
			}
			for (int dy = 0; dy < h - 3; dy++) {                       // a ladder up the inside
				set(level, base.offset(0, dy, 1), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
			}
			set(level, base.offset(1, h - 4, 1), ModBlocks.SCULK_STONE_BRICKS.defaultBlockState());
			if (random.nextInt(2) == 0) {
				chest(level, random, base.offset(1, h - 3, 1), CITY);
			}
		}

		private static void colonnade(WorldGenLevel level, RandomSource random, BlockPos base) {
			boolean alongX = random.nextBoolean();
			for (int i = -6; i <= 6; i += 3) {
				for (int side = -2; side <= 2; side += 4) {
					BlockPos column = alongX ? base.offset(i, 0, side) : base.offset(side, 0, i);
					foundation(level, column.below());
					int h = 3 + random.nextInt(4);
					for (int dy = 0; dy < h; dy++) {
						set(level, column.above(dy), dy == h - 1 && random.nextBoolean() ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
								: Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
					}
				}
				if (random.nextInt(3) == 0) {
					soft(level, (alongX ? base.offset(i, 0, 0) : base.offset(0, 0, i)), ModBlocks.ECHO_GLASS.defaultBlockState());
				}
			}
		}
	}

	// ================================================================ the Bell Valley

	/**
	 * A great cracked bell of old green copper hanging on chains from a shape of bone: an arch, a claw curling over, a
	 * leaning spine, or two bones twisting round each other. The Bell Core inside makes it toll.
	 */
	public static final class BoneBell extends Unit {
		public static final BoneBell INSTANCE = new BoneBell();
		public static final MapCodec<BoneBell> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		private static BlockState bone(RandomSource random) {
			int r = random.nextInt(12);
			return r < 8 ? Blocks.BONE_BLOCK.defaultBlockState() : r < 10 ? Blocks.SCULK.defaultBlockState()
					: r < 11 ? ModBlocks.SCULK_STONE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
		}

		private static void blob(WorldGenLevel level, RandomSource random, BlockPos c, float r) {
			int ir = Mth.ceil(r);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int dx = -ir; dx <= ir; dx++) {
				for (int dy = -ir; dy <= ir; dy++) {
					for (int dz = -ir; dz <= ir; dz++) {
						if (dx * dx + dy * dy + dz * dz <= r * r + 0.5F) {
							set(level, pos.setWithOffset(c, dx, dy, dz), bone(random));
						}
					}
				}
			}
		}

		/** A bone along a path of points, thick at the start, thinner towards the end, with a knuckle now and then. */
		private static void boneAlong(WorldGenLevel level, RandomSource random, double[][] points, float thick, float thin) {
			for (int i = 0; i < points.length; i++) {
				float t = i / (float) Math.max(1, points.length - 1);
				float r = thick + (thin - thick) * t;
				if (i % 5 == 0) {
					r += 0.8F;                                          // joints
				}
				blob(level, random, BlockPos.containing(points[i][0], points[i][1], points[i][2]), r);
			}
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			double ox = origin.getX() + 0.5;
			double oz = origin.getZ() + 0.5;
			double angle = random.nextDouble() * Math.PI;
			double ax = Math.cos(angle);
			double az = Math.sin(angle);
			int height = 12 + random.nextInt(9);
			int steps = 40;
			double[] hang;
			switch (random.nextInt(4)) {
				case 0 -> {                                              // an arch: two bones meeting overhead
					double span = 5 + random.nextInt(4);
					double[][] a = new double[steps][];
					double[][] b = new double[steps][];
					for (int i = 0; i < steps; i++) {
						double t = i / (double) (steps - 1);
						double s = Math.sin(t * Math.PI / 2);
						a[i] = new double[] {ox - ax * span * (1 - s), y + height * Math.sin(t * Math.PI / 2), oz - az * span * (1 - s)};
						b[i] = new double[] {ox + ax * span * (1 - s), y + height * Math.sin(t * Math.PI / 2), oz + az * span * (1 - s)};
					}
					boneAlong(level, random, a, 1.6F, 1.0F);
					boneAlong(level, random, b, 1.6F, 1.0F);
					hang = new double[] {ox, y + height - 1, oz};
				}
				case 1 -> {                                              // a claw: one bone rising and curling over
					double[][] a = new double[steps][];
					for (int i = 0; i < steps; i++) {
						double t = i / (double) (steps - 1);
						double curl = t * t * 1.6;
						a[i] = new double[] {ox + ax * Math.sin(curl) * height * 0.55, y + Math.sin(Math.min(1.2, t * 1.8)) * height,
								oz + az * Math.sin(curl) * height * 0.55};
					}
					boneAlong(level, random, a, 2.0F, 0.6F);
					double[] tip = a[steps - 1];
					hang = new double[] {tip[0], tip[1] - 1, tip[2]};
				}
				case 2 -> {                                              // a leaning spine with ribs sticking out
					double lean = 0.3 + random.nextDouble() * 0.15;          // kept inside the chunks a feature may reach
					double[][] a = new double[steps][];
					for (int i = 0; i < steps; i++) {
						double t = i / (double) (steps - 1);
						a[i] = new double[] {ox + ax * lean * t * height, y + t * height, oz + az * lean * t * height};
					}
					boneAlong(level, random, a, 1.5F, 1.2F);
					for (int i = 8; i < steps; i += 6) {
						for (int side = -1; side <= 1; side += 2) {
							double[][] rib = new double[5][];
							for (int k = 0; k < 5; k++) {
								rib[k] = new double[] {a[i][0] - az * side * k * 0.6, a[i][1] - k * 0.3, a[i][2] + ax * side * k * 0.6};
							}
							boneAlong(level, random, rib, 0.6F, 0.3F);
						}
					}
					double[] top = a[steps - 1];
					hang = new double[] {top[0] + ax * 3, top[1] - 1, top[2] + az * 3};
					boneAlong(level, random, new double[][] {top, {top[0] + ax * 1.5, top[1] + 0.5, top[2] + az * 1.5}, {hang[0], hang[1] + 1, hang[2]}}, 1.0F, 0.8F);
				}
				default -> {                                             // two bones twisting round each other
					double[][] a = new double[steps][];
					double[][] b = new double[steps][];
					for (int i = 0; i < steps; i++) {
						double t = i / (double) (steps - 1);
						double turn = t * Math.PI * 2.2;
						double r = 3.0 * (1 - t * 0.7);
						a[i] = new double[] {ox + Math.cos(turn) * r, y + t * height, oz + Math.sin(turn) * r};
						b[i] = new double[] {ox - Math.cos(turn) * r, y + t * height, oz - Math.sin(turn) * r};
					}
					boneAlong(level, random, a, 1.2F, 0.7F);
					boneAlong(level, random, b, 1.2F, 0.7F);
					double[] top = a[steps - 1];
					hang = new double[] {top[0] + 3, top[1], top[2]};
					boneAlong(level, random, new double[][] {top, {top[0] + 1.5, top[1] + 0.6, top[2]}, {hang[0], hang[1], hang[2]}}, 0.9F, 0.8F);
				}
			}
			bell(level, random, BlockPos.containing(hang[0], hang[1], hang[2]).below(), y);
			return true;
		}

		/** Chains down from the bone, then the bell: a cap, widening sides and a broken rim, with its core inside. */
		private static void bell(WorldGenLevel level, RandomSource random, BlockPos top, int ground) {
			BlockState chain = state("minecraft:iron_chain");
			int chainLength = 1 + random.nextInt(3);
			BlockPos.MutableBlockPos pos = top.mutable();
			for (int i = 0; i < chainLength; i++) {
				soft(level, pos, chain);
				pos.move(Direction.DOWN);
			}
			BlockState[] copper = {state("minecraft:waxed_oxidized_cut_copper"), state("minecraft:waxed_weathered_cut_copper"),
					state("minecraft:waxed_oxidized_copper")};
			BlockPos cap = pos.immutable();
			int rings = 4;
			if (cap.getY() - rings - 1 <= ground) {
				return;                                              // no room for the bell under this bone
			}
			set(level, cap, copper[0]);
			for (int ring = 1; ring <= rings; ring++) {
				int r = ring < 3 ? 1 : 2;
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						boolean shell = Math.abs(dx) == r || Math.abs(dz) == r;
						if (r == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2) {
							continue;                                // rounded corners
						}
						BlockPos p = cap.offset(dx, -ring, dz);
						if (shell) {
							if (ring == rings && random.nextInt(3) == 0) {
								continue;                            // the cracked, broken rim
							}
							soft(level, p, copper[random.nextInt(copper.length)]);
						} else if (ring == 1) {
							set(level, p, ModBlocks.BELL_CORE.defaultBlockState());
						}
					}
				}
			}
		}
	}

	// ================================================================ the Listening Pillars

	/**
	 * A tall pointed pillar of Sculk Stone with darker bands, most of them thin, some thick, a few hollow with holes the
	 * wind sings through. Thorn Vine climbs some, Ash Flowers grow at their feet; the tallest thick ones may carry a
	 * Spire Nester's nest, and rarely one is a Hermit Pillar: hollow, with a stair inside to a room at the top.
	 */
	public static final class Pillar extends Unit {
		public static final Pillar INSTANCE = new Pillar();
		public static final MapCodec<Pillar> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			BlockPos base = new BlockPos(origin.getX(), y - 2, origin.getZ());
			boolean thick = random.nextInt(4) == 0;
			if (thick && random.nextInt(10) == 0) {
				hermit(level, random, base);
				return true;
			}
			float radius = thick ? 2.0F + random.nextFloat() * 1.2F : 0.6F + random.nextFloat() * 0.6F;
			int height = thick ? 35 + random.nextInt(36) : 18 + random.nextInt(28);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int ir = Mth.ceil(radius);
			int band = random.nextInt(5);
			for (int dy = 0; dy < height; dy++) {
				float t = dy / (float) height;
				float r = radius * (t < 0.7F ? 1.0F - t * 0.25F : (1.0F - t) / 0.3F * 0.82F) + 0.25F;
				BlockState stone = (dy + band) % 9 < 2 ? Blocks.DEEPSLATE.defaultBlockState() : ModBlocks.SCULK_STONE.defaultBlockState();
				for (int dx = -ir; dx <= ir; dx++) {
					for (int dz = -ir; dz <= ir; dz++) {
						if (dx * dx + dz * dz <= r * r) {
							set(level, pos.setWithOffset(base, dx, dy, dz), stone);
						}
					}
				}
			}
			set(level, base.above(height), Blocks.SCULK_CATALYST.defaultBlockState());   // the faint light on its tip
			if (thick) {                                                   // holes the wind sings through
				for (int i = 0; i < 2 + random.nextInt(3); i++) {
					int hy = 6 + random.nextInt(Math.max(1, height / 2));
					boolean alongX = random.nextBoolean();
					for (int k = -ir; k <= ir; k++) {
						for (int up = 0; up < 2; up++) {
							set(level, alongX ? base.offset(k, hy + up, 0) : base.offset(0, hy + up, k), Blocks.AIR.defaultBlockState());
						}
					}
				}
				if (random.nextInt(3) == 0) {
					nest(level, random, base.above(height - 2), Math.max(1, (int) (radius * 0.6F)));
				}
			}
			vines(level, random, base, ir, height);
			for (int i = 0; i < 4; i++) {                                    // Ash Flowers at its foot
				BlockPos flower = base.offset(random.nextInt(ir * 2 + 5) - ir - 2, 0, random.nextInt(ir * 2 + 5) - ir - 2);
				flower = new BlockPos(flower.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, flower.getX(), flower.getZ()), flower.getZ());
				if (level.isEmptyBlock(flower) && hu.endrealms.sculk.SculkPlants.ground(level.getBlockState(flower.below()))) {
					set(level, flower, ModBlocks.ASH_FLOWER.defaultBlockState());
				}
			}
			return true;
		}

		/** Thorn Vine climbing one side of the pillar, from its foot up a good way. */
		private static void vines(WorldGenLevel level, RandomSource random, BlockPos base, int ir, int height) {
			if (random.nextInt(3) != 0) {
				return;
			}
			Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int length = 6 + random.nextInt(Math.max(1, height / 2));
			for (int dy = 2; dy < length; dy++) {
				int out = 0;
				pos.setWithOffset(base, 0, dy, 0);
				while (out <= ir + 1 && !level.getBlockState(pos).isAir()) {
					pos.move(side);
					out++;
				}
				if (level.getBlockState(pos).isAir() && level.getBlockState(pos.relative(side.getOpposite())).isSolid()) {
					set(level, pos, ModBlocks.THORN_VINE.defaultBlockState().setValue(LadderBlock.FACING, side)
							.setValue(SculkWilds.ThornVine.BERRIES, random.nextInt(5) == 0));
				}
			}
		}

		/** A Spire Nester's nest on a pillar top: a ring of sticks and bone, now and then a hoard, and its owners about. */
		private static void nest(WorldGenLevel level, RandomSource random, BlockPos top, int r) {
			BlockState sticks = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
			for (int dx = -r - 1; dx <= r + 1; dx++) {
				for (int dz = -r - 1; dz <= r + 1; dz++) {
					int d = Math.max(Math.abs(dx), Math.abs(dz));
					BlockPos p = top.offset(dx, 1, dz);
					if (d == r + 1) {
						soft(level, p, random.nextInt(3) == 0 ? Blocks.BONE_BLOCK.defaultBlockState() : sticks);
					}
				}
			}
			if (random.nextInt(2) == 0) {
				chest(level, random, top.above(), NEST);
			}
			for (int i = 0; i < 1 + random.nextInt(2); i++) {
				spawn(level, ModEntities.SPIRE_NESTER, top.above(3 + i), random);
			}
		}

		/** A thick hollow pillar with a door at the foot, a winding stair inside and a small room at the top. */
		private static void hermit(WorldGenLevel level, RandomSource random, BlockPos base) {
			int height = 30 + random.nextInt(12);
			int r = 3;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int dy = 0; dy < height + 4; dy++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						int d2 = dx * dx + dz * dz;
						if (d2 > r * r + 1) {
							continue;
						}
						pos.setWithOffset(base, dx, dy, dz);
						boolean wall = d2 >= (r - 1) * (r - 1) + 1;
						if (dy >= height + 3) {
							set(level, pos, Blocks.DEEPSLATE_TILES.defaultBlockState());        // the roof
						} else if (wall) {
							set(level, pos, dy % 9 < 2 ? Blocks.DEEPSLATE.defaultBlockState() : ModBlocks.SCULK_STONE.defaultBlockState());
						} else {
							set(level, pos, Blocks.AIR.defaultBlockState());
						}
					}
				}
			}
			for (int dy = 2; dy <= 3; dy++) {                                   // the door
				set(level, base.offset(0, dy, -r), Blocks.AIR.defaultBlockState());
				set(level, base.offset(0, dy, -r + 1), Blocks.AIR.defaultBlockState());
			}
			set(level, base.offset(0, 1, -r), ModBlocks.SCULK_STONE_BRICKS.defaultBlockState());
			int[][] ring = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};       // the stair winding up
			Direction[] facing = {Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH};
			for (int step = 0; step < height - 1; step++) {
				int[] c = ring[step % 8];
				set(level, base.offset(c[0], 2 + step, c[1]), Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing[step % 8]));
				set(level, base.offset(0, 2 + step, 0), ModBlocks.SCULK_STONE_BRICKS.defaultBlockState());
			}
			for (int dx = -2; dx <= 2; dx++) {                                   // the room's floor
				for (int dz = -2; dz <= 2; dz++) {
					if (dx * dx + dz * dz <= 5) {
						set(level, base.offset(dx, height, dz), Blocks.DEEPSLATE_TILES.defaultBlockState());
					}
				}
			}
			set(level, base.offset(0, height, 0), Blocks.AIR.defaultBlockState());   // the stair comes up through the middle
			chest(level, random, base.offset(1, height + 1, 1), HERMIT);
			set(level, base.offset(-1, height + 1, 1), Blocks.CANDLE.defaultBlockState());
			for (int i = 0; i < 4; i++) {                                         // windows
				Direction d = Direction.from2DDataValue(i);
				set(level, base.relative(d, r).above(height + 1), ModBlocks.ECHO_GLASS.defaultBlockState());
			}
		}
	}

	// ================================================================ the Scattered Ruins

	/**
	 * Ruins that belong to nothing: a piece of wall, a house corner, a row of broken columns, a lone arch, a dry well, a
	 * stair to nowhere, a toppled tower, a fallen statue, an altar. Each of its own odd mix of stone; some keep a chest,
	 * some are watched over by something unpleasant.
	 */
	public static final class Ruin extends Unit {
		public static final Ruin INSTANCE = new Ruin();
		public static final MapCodec<Ruin> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		private static BlockState[] palette(RandomSource random) {
			BlockState[][] all = {
					{ModBlocks.SCULK_STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), ModBlocks.SCULK_STONE.defaultBlockState()},
					{Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState()},
					{Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState()},
					{Blocks.TUFF_BRICKS.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.CHISELED_TUFF.defaultBlockState()},
					{Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState(), ModBlocks.ECHO_GLASS.defaultBlockState()}};
			return all[random.nextInt(all.length)];
		}

		private static BlockState pick(BlockState[] palette, RandomSource random) {
			return palette[random.nextInt(10) < 6 ? 0 : random.nextInt(palette.length)];
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			BlockPos base = new BlockPos(origin.getX(), y, origin.getZ());
			BlockState[] p = palette(random);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			boolean alongX = random.nextBoolean();
			switch (random.nextInt(9)) {
				case 0 -> {                                                     // a piece of wall
					int len = 5 + random.nextInt(6);
					for (int i = -len / 2; i <= len / 2; i++) {
						int h = 2 + random.nextInt(4);
						for (int dy = -1; dy < h; dy++) {
							set(level, alongX ? pos.setWithOffset(base, i, dy, 0) : pos.setWithOffset(base, 0, dy, i), pick(p, random));
						}
					}
				}
				case 1 -> {                                                     // a house corner
					for (int i = 0; i < 6; i++) {
						int h = 4 - i / 2 + random.nextInt(2);
						for (int dy = -1; dy < h; dy++) {
							set(level, pos.setWithOffset(base, i, dy, 0), pick(p, random));
							set(level, pos.setWithOffset(base, 0, dy, i), pick(p, random));
						}
					}
				}
				case 2 -> {                                                     // a row of broken columns
					for (int i = -6; i <= 6; i += 3) {
						int h = 1 + random.nextInt(5);
						for (int dy = -1; dy < h; dy++) {
							set(level, alongX ? pos.setWithOffset(base, i, dy, 0) : pos.setWithOffset(base, 0, dy, i), pick(p, random));
						}
					}
				}
				case 3 -> {                                                     // a lone arch
					for (int i = -3; i <= 3; i++) {
						int top = 6;
						for (int dy = -1; dy <= top; dy++) {
							if (Math.abs(i) == 3 || dy >= 5) {
								set(level, alongX ? pos.setWithOffset(base, i, dy, 0) : pos.setWithOffset(base, 0, dy, i), pick(p, random));
							}
						}
					}
				}
				case 4 -> {                                                     // a dry well, deeper than it looks
					for (int dx = -2; dx <= 2; dx++) {
						for (int dz = -2; dz <= 2; dz++) {
							boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
							for (int dy = -9; dy <= 1; dy++) {
								pos.setWithOffset(base, dx, dy, dz);
								if (ring) {
									if (dy < 1 || (dx + dz) % 2 == 0) {
										set(level, pos, pick(p, random));
									}
								} else {
									set(level, pos, dy == -9 ? Blocks.SOUL_SOIL.defaultBlockState() : Blocks.AIR.defaultBlockState());
								}
							}
						}
					}
					if (random.nextInt(2) == 0) {
						chest(level, random, base.offset(0, -8, 0), RUIN);
					}
				}
				case 5 -> {                                                     // a stair to nowhere
					for (int i = 0; i < 7; i++) {
						for (int w = -1; w <= 1; w++) {
							BlockPos step = alongX ? base.offset(i, i, w) : base.offset(w, i, i);
							for (int dy = -1; dy <= 0; dy++) {
								set(level, step.above(dy - i), pick(p, random));
							}
							for (int dy = 0; dy < i; dy++) {
								set(level, step.below(i - dy), pick(p, random));
							}
						}
					}
				}
				case 6 -> {                                                     // a toppled tower lying on its side
					for (int i = 0; i < 12; i++) {
						for (int a = -2; a <= 2; a++) {
							for (int b = 0; b <= 4; b++) {
								boolean shell = Math.abs(a) == 2 || b == 0 || b == 4;
								if (shell && random.nextInt(6) != 0) {
									set(level, alongX ? pos.setWithOffset(base, i, b - 1, a) : pos.setWithOffset(base, a, b - 1, i), pick(p, random));
								}
							}
						}
					}
				}
				case 7 -> {                                                     // a fallen statue: a stone figure face down
					BlockState s = p[0];
					for (int i = 0; i < 8; i++) {
						set(level, alongX ? pos.setWithOffset(base, i, 0, 0) : pos.setWithOffset(base, 0, 0, i), s);          // the body
						if (i >= 3 && i < 8) {
							set(level, alongX ? pos.setWithOffset(base, i, 0, -1) : pos.setWithOffset(base, -1, 0, i), s);
							set(level, alongX ? pos.setWithOffset(base, i, 0, 1) : pos.setWithOffset(base, 1, 0, i), s);
						}
					}
					for (int dx = -1; dx <= 0; dx++) {
						for (int dz = 0; dz <= 1; dz++) {
							set(level, alongX ? pos.setWithOffset(base, -2 + dx, dz, 0) : pos.setWithOffset(base, 0, dz, -2 + dx), s);   // the head
						}
					}
					soft(level, base.above(), Blocks.SCULK_VEIN.defaultBlockState());
				}
				default -> {                                                    // an altar with unlit candles
					for (int dx = -2; dx <= 2; dx++) {
						for (int dz = -2; dz <= 2; dz++) {
							set(level, pos.setWithOffset(base, dx, -1, dz), pick(p, random));
						}
					}
					for (int dx = -1; dx <= 1; dx++) {
						set(level, base.offset(dx, 0, 0), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
					}
					soft(level, base.offset(-1, 1, 0), Blocks.CANDLE.defaultBlockState());
					soft(level, base.offset(1, 1, 0), Blocks.CANDLE.defaultBlockState());
					if (random.nextInt(2) == 0) {
						chest(level, random, base.offset(0, 1, 0), RUIN);
					}
				}
			}
			if (random.nextInt(3) == 0) {                                        // now and then a chest half buried by it
				BlockPos c = base.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
				if (level.getBlockState(c).isAir() || level.getBlockState(c).canBeReplaced()) {
					chest(level, random, c, RUIN);
				}
			}
			if (random.nextInt(4) == 0) {                                        // and now and then something waiting by it
				EntityType<? extends Mob> guard = random.nextBoolean() ? ModEntities.PILLAR_CRAWLER : ModEntities.BELLRINGER;
				BlockPos g = base.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
				g = new BlockPos(g.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, g.getX(), g.getZ()), g.getZ());
				spawn(level, guard, g, random);
			}
			return true;
		}
	}

	public static void register() {
		net.minecraft.core.Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("hanging_island"), HangingIsland.CODEC);
		net.minecraft.core.Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("city_building"), CityBuilding.CODEC);
		net.minecraft.core.Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("bone_bell"), BoneBell.CODEC);
		net.minecraft.core.Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("listening_pillar"), Pillar.CODEC);
		net.minecraft.core.Registry.register(BuiltInRegistries.FEATURE_TYPE, EndRealms.id("sculk_ruin"), Ruin.CODEC);
	}

}
