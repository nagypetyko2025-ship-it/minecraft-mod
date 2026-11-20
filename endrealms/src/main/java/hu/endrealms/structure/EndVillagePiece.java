package hu.endrealms.structure;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.EndVillager;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModFluids;
import hu.endrealms.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * One part of an End Village: the plaza, a path, or a house. Houses are built in local coordinates (a across the
 * front, b from the door side to the back) so the same plans work for all four facings. Everything comes from the
 * stored values and seed, so each chunk builds its share of the same building.
 */
public class EndVillagePiece extends StructurePiece {
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/end_village"));
	private static final int KIND_PLAZA = 0;
	private static final int KIND_PATH = 1;
	private static final int KIND_HOUSE = 2;
	private static final int FOUNDATION_DEPTH = 16;
	private static final int PLAZA_RADIUS = 7;
	private static final int STYLE_COUNT = 3;

	private final int kind;
	private final int x;
	private final int y;
	private final int z;
	/** Path: the far end. House: half width, half depth, wall height. */
	private final int p1;
	private final int p2;
	private final int p3;
	private final int facing;
	private final int style;
	private final int profession;
	private final long seed;

	private EndVillagePiece(int kind, int x, int y, int z, int p1, int p2, int p3, int facing, int style, int profession, long seed, BoundingBox box) {
		super(ModStructures.END_VILLAGE_PIECE, 0, box);
		this.kind = kind;
		this.x = x;
		this.y = y;
		this.z = z;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.facing = facing;
		this.style = style;
		this.profession = profession;
		this.seed = seed;
	}

	public EndVillagePiece(CompoundTag tag) {
		super(ModStructures.END_VILLAGE_PIECE, tag);
		this.kind = tag.getIntOr("Kind", KIND_PLAZA);
		this.x = tag.getIntOr("X0", 0);
		this.y = tag.getIntOr("Y0", 64);
		this.z = tag.getIntOr("Z0", 0);
		this.p1 = tag.getIntOr("P1", 0);
		this.p2 = tag.getIntOr("P2", 0);
		this.p3 = tag.getIntOr("P3", 0);
		this.facing = tag.getIntOr("Facing", 2);
		this.style = tag.getIntOr("Style", 0);
		this.profession = tag.getIntOr("Profession", 0);
		this.seed = tag.getLongOr("Seed", 0L);
	}

	static EndVillagePiece plaza(RandomSource random, int x, int y, int z) {
		int r = PLAZA_RADIUS + 1;
		return new EndVillagePiece(KIND_PLAZA, x, y, z, 0, 0, 0, 0, 0, 0, random.nextLong(),
				new BoundingBox(x - r, y - FOUNDATION_DEPTH, z - r, x + r, y + 8, z + r));
	}

	static EndVillagePiece path(int x1, int y1, int z1, int x2, int y2, int z2) {
		return new EndVillagePiece(KIND_PATH, x1, y1, z1, x2, y2, z2, 0, 0, 0, 0L,
				new BoundingBox(Math.min(x1, x2) - 1, Math.min(y1, y2) - 24, Math.min(z1, z2) - 1, Math.max(x1, x2) + 1, Math.max(y1, y2) + 24, Math.max(z1, z2) + 1));
	}

	static EndVillagePiece house(RandomSource random, int x, int y, int z, Direction facing, EndVillager.Profession profession) {
		int halfWidth = 3 + random.nextInt(2);
		int halfDepth = 3 + random.nextInt(2);
		int style = random.nextInt(STYLE_COUNT);
		int wallHeight = style == 1 ? 6 : 4;
		int r = Math.max(halfWidth, halfDepth) + 3;
		return new EndVillagePiece(KIND_HOUSE, x, y, z, halfWidth, halfDepth, wallHeight, facing.get2DDataValue(), style, profession.ordinal(), random.nextLong(),
				new BoundingBox(x - r, y - FOUNDATION_DEPTH, z - r, x + r, y + wallHeight + halfWidth + 5, z + r));
	}

	/** The block just outside a house door, where its path begins. */
	BlockPos frontOfDoor() {
		Direction direction = Direction.from2DDataValue(facing);
		return new BlockPos(x, y, z).relative(direction, p2 + 2);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("Kind", kind);
		tag.putInt("X0", x);
		tag.putInt("Y0", y);
		tag.putInt("Z0", z);
		tag.putInt("P1", p1);
		tag.putInt("P2", p2);
		tag.putInt("P3", p3);
		tag.putInt("Facing", facing);
		tag.putInt("Style", style);
		tag.putInt("Profession", profession);
		tag.putLong("Seed", seed);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource unused,
			BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		switch (kind) {
			case KIND_PLAZA -> buildPlaza(level, chunkBox);
			case KIND_PATH -> buildPath(level, chunkBox);
			default -> buildHouse(level, chunkBox);
		}
	}

	private static void put(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
		if (box.isInside(pos)) {
			level.setBlock(pos, state, 2);
		}
	}

	/** Fills downwards from pos until it meets solid ground, so nothing floats above a dip in the island. */
	private static void foundation(WorldGenLevel level, BoundingBox box, BlockPos top, BlockState state) {
		BlockPos.MutableBlockPos pos = top.mutable();
		for (int i = 0; i < FOUNDATION_DEPTH; i++) {
			pos.move(Direction.DOWN);
			if (!box.isInside(pos) || !level.getBlockState(pos).canBeReplaced()) {
				return;
			}
			level.setBlock(pos, state, 2);
		}
	}

	// ---------------------------------------------------------------- plaza

	private void buildPlaza(WorldGenLevel level, BoundingBox box) {
		BlockState bricks = Blocks.END_STONE_BRICKS.defaultBlockState();
		BlockState stone = Blocks.END_STONE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int dx = -PLAZA_RADIUS; dx <= PLAZA_RADIUS; dx++) {
			for (int dz = -PLAZA_RADIUS; dz <= PLAZA_RADIUS; dz++) {
				if (dx * dx + dz * dz > PLAZA_RADIUS * PLAZA_RADIUS + 2) {
					continue;
				}
				BlockPos floor = new BlockPos(x + dx, y, z + dz);
				boolean ring = Math.abs(Math.max(Math.abs(dx), Math.abs(dz)) - 5) == 0;
				put(level, box, floor, ring ? ModBlocks.ASTRAL_PLANKS.defaultBlockState() : bricks);
				foundation(level, box, floor, stone);
				for (int h = 1; h <= 8; h++) {
					put(level, box, floor.above(h), air);
				}
			}
		}
		// fountain: an astral water basin (never plain water here) around a lamp column
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				boolean rim = Math.max(Math.abs(dx), Math.abs(dz)) == 3;
				BlockPos pos = new BlockPos(x + dx, y + 1, z + dz);
				put(level, box, pos, rim ? Blocks.END_STONE_BRICK_WALL.defaultBlockState() : ModFluids.ASTRAL_WATER_BLOCK.defaultBlockState());
			}
		}
		for (int h = 1; h <= 3; h++) {
			put(level, box, new BlockPos(x, y + h, z), Blocks.END_STONE_BRICKS.defaultBlockState());
		}
		put(level, box, new BlockPos(x, y + 4, z), ModBlocks.PRISM_LAMP.defaultBlockState());
		put(level, box, new BlockPos(x, y + 5, z), Blocks.END_ROD.defaultBlockState());
		for (int[] corner : new int[][] {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
			lampPost(level, box, x + corner[0], y + 1, z + corner[1]);
		}
	}

	private static void lampPost(WorldGenLevel level, BoundingBox box, int px, int py, int pz) {
		for (int h = 0; h < 3; h++) {
			put(level, box, new BlockPos(px, py + h, pz), ModBlocks.ASTRAL_FENCE.defaultBlockState());
		}
		put(level, box, new BlockPos(px, py + 3, pz), ModBlocks.PRISM_LAMP.defaultBlockState());
	}

	// ---------------------------------------------------------------- paths

	private void buildPath(WorldGenLevel level, BoundingBox box) {
		int length = Math.abs(p1 - x) + Math.abs(p3 - z);
		int cornerX = p1;
		int cornerZ = z;
		for (int step = 0; step <= length; step++) {
			int stepsX = Math.min(step, Math.abs(cornerX - x));
			int stepsZ = step - stepsX;
			int px = x + Integer.signum(cornerX - x) * stepsX;
			int pz = z + Integer.signum(p3 - cornerZ) * stepsZ;
			int planned = Mth.lerpInt((float) step / Math.max(1, length), y, p2);
			for (int side = -1; side <= 1; side++) {
				for (int other = -1; other <= 1; other++) {
					paveColumn(level, box, px + side, pz + other, planned);
				}
			}
		}
	}

	private static void paveColumn(WorldGenLevel level, BoundingBox box, int px, int pz, int planned) {
		if (!box.isInside(px, box.minY(), pz)) {
			return;
		}
		int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, px, pz) - 1;
		if (ground < planned - 10) {       // a gap in the island: carry the path across on a plank bridge
			put(level, box, new BlockPos(px, planned, pz), ModBlocks.ASTRAL_PLANKS.defaultBlockState());
			return;
		}
		Block block = ((px * 31 + pz * 17) & 3) == 0 ? Blocks.END_STONE : Blocks.END_STONE_BRICKS;
		put(level, box, new BlockPos(px, ground, pz), block.defaultBlockState());
	}

	// ---------------------------------------------------------------- houses

	private BlockPos local(int a, int h, int b) {
		return switch (Direction.from2DDataValue(facing)) {
			case EAST -> new BlockPos(x - b, y + h, z + a);
			case SOUTH -> new BlockPos(x - a, y + h, z - b);
			case WEST -> new BlockPos(x + b, y + h, z - a);
			default -> new BlockPos(x + a, y + h, z + b);
		};
	}

	private void buildHouse(WorldGenLevel level, BoundingBox box) {
		RandomSource random = RandomSource.create(seed);
		Direction front = Direction.from2DDataValue(facing);
		int w = p1;
		int d = p2;
		int wall = p3;
		BlockState bricks = Blocks.END_STONE_BRICKS.defaultBlockState();
		BlockState stone = Blocks.END_STONE.defaultBlockState();
		BlockState planks = ModBlocks.ASTRAL_PLANKS.defaultBlockState();
		BlockState log = ModBlocks.ASTRAL_LOG.defaultBlockState();
		BlockState glass = Blocks.STAINED_GLASS.purple().defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		for (int a = -w; a <= w; a++) {
			for (int b = -d; b <= d; b++) {
				boolean edgeA = Math.abs(a) == w;
				boolean edgeB = Math.abs(b) == d;
				boolean corner = edgeA && edgeB;
				BlockPos floor = local(a, 0, b);
				put(level, box, floor, edgeA || edgeB ? bricks : planks);
				foundation(level, box, floor, stone);
				for (int h = 1; h <= wall + w + 4; h++) {
					put(level, box, local(a, h, b), air);
				}
				for (int h = 1; h <= wall; h++) {
					BlockState state = air;
					if (corner) {
						state = log;
					} else if (edgeA || edgeB) {
						boolean window = h >= 2 && h <= wall - 1 && h <= 3 && (edgeA ? Math.abs(b) <= 1 && b != 0 || b == 0 : Math.abs(a) == 2) && !(edgeB && b < 0 && a == 0);
						boolean upper = style != 0 && h > (style == 1 ? 3 : 2);
						state = window ? glass : upper ? planks : h == 1 ? stone : bricks;
					}
					put(level, box, local(a, h, b), state);
				}
			}
		}

		// front door with a landing in front of it
		DoorBlock door = (DoorBlock) ModBlocks.ASTRAL_DOOR;
		BlockState doorState = door.defaultBlockState().setValue(DoorBlock.FACING, front.getOpposite());
		put(level, box, local(0, 1, -d), doorState.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		put(level, box, local(0, 2, -d), doorState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		for (int a = -1; a <= 1; a++) {
			BlockPos landing = local(a, 0, -d - 1);
			put(level, box, landing, bricks);
			foundation(level, box, landing, stone);
			put(level, box, local(a, 1, -d - 1), air);
			put(level, box, local(a, 2, -d - 1), air);
		}
		put(level, box, local(-1, 3, -d - 1), Blocks.END_ROD.defaultBlockState());
		put(level, box, local(1, 3, -d - 1), Blocks.END_ROD.defaultBlockState());

		if (style == 1) {
			buildFlatRoof(level, box, w, d, wall);
		} else {
			buildGableRoof(level, box, w, d, wall);
		}
		furnish(level, box, random, w, d, wall);
	}

	/** Ridge runs from the door side to the back; both gable ends are filled with planks. */
	private void buildGableRoof(WorldGenLevel level, BoundingBox box, int w, int d, int wall) {
		Direction front = Direction.from2DDataValue(facing);
		BlockState stairs = ModBlocks.ASTRAL_STAIRS.defaultBlockState();
		BlockState planks = ModBlocks.ASTRAL_PLANKS.defaultBlockState();
		for (int layer = 0; layer <= w; layer++) {
			int h = wall + 1 + layer;
			int a = w + 1 - layer;
			for (int b = -d - 1; b <= d + 1; b++) {
				if (a > 0) {
					put(level, box, local(a, h, b), stairs.setValue(StairBlock.FACING, front.getCounterClockWise()));
					put(level, box, local(-a, h, b), stairs.setValue(StairBlock.FACING, front.getClockWise()));
				} else {
					put(level, box, local(0, h, b), ModBlocks.ASTRAL_SLAB.defaultBlockState());
				}
			}
			if (a > 0) {
				for (int fill = -a + 1; fill <= a - 1; fill++) {
					put(level, box, local(fill, h, -d), planks);
					put(level, box, local(fill, h, d), planks);
				}
			}
		}
		// attic floor doubles as the ceiling and hides the roof's underside
		for (int a = -w + 1; a < w; a++) {
			for (int b = -d + 1; b < d; b++) {
				put(level, box, local(a, wall + 1, b), planks);
			}
		}
	}

	private void buildFlatRoof(WorldGenLevel level, BoundingBox box, int w, int d, int wall) {
		BlockState planks = ModBlocks.ASTRAL_PLANKS.defaultBlockState();
		for (int a = -w; a <= w; a++) {
			for (int b = -d; b <= d; b++) {
				put(level, box, local(a, wall + 1, b), planks);
				if (Math.abs(a) == w || Math.abs(b) == d) {
					boolean merlon = ((a + b) & 1) == 0;
					put(level, box, local(a, wall + 2, b), merlon ? Blocks.END_STONE_BRICK_WALL.defaultBlockState() : Blocks.END_STONE_BRICK_SLAB.defaultBlockState());
				}
			}
		}
		for (int[] c : new int[][] {{-w, -d}, {w, -d}, {-w, d}, {w, d}}) {
			put(level, box, local(c[0], wall + 3, c[1]), Blocks.END_ROD.defaultBlockState());
		}
	}

	private static BlockState workstation(EndVillager.Profession profession) {
		return switch (profession) {
			case TOOLSMITH -> Blocks.SMITHING_TABLE.defaultBlockState();
			case CASTAWAY -> Blocks.BARREL.defaultBlockState();
			case MASON -> Blocks.STONECUTTER.defaultBlockState();
			case BUTCHER -> Blocks.SMOKER.defaultBlockState();
			case FISHERMAN -> Blocks.BARREL.defaultBlockState();
			case CLERIC -> Blocks.BREWING_STAND.defaultBlockState();
			case LIBRARIAN -> Blocks.LECTERN.defaultBlockState();
			case FORESTER -> Blocks.FLETCHING_TABLE.defaultBlockState();
			case JEWELER -> Blocks.CARTOGRAPHY_TABLE.defaultBlockState();
		};
	}

	private void furnish(WorldGenLevel level, BoundingBox box, RandomSource random, int w, int d, int wall) {
		EndVillager.Profession job = EndVillager.Profession.values()[profession];
		Direction front = Direction.from2DDataValue(facing);
		int back = d - 1;
		put(level, box, local(0, 1, back), workstation(job));
		if (job == EndVillager.Profession.LIBRARIAN) {
			for (int a = -w + 1; a < w; a++) {
				if (a != 0) {
					put(level, box, local(a, 1, back), Blocks.BOOKSHELF.defaultBlockState());
					put(level, box, local(a, 2, back), Blocks.BOOKSHELF.defaultBlockState());
				}
			}
		} else if (random.nextBoolean()) {
			chest(level, box, w - 1, 1, back, front);
		}
		put(level, box, local(-w + 1, 1, back), Blocks.BARREL.defaultBlockState());

		// purple rug down the middle, a lamp set in the ceiling
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				put(level, box, local(a, 1, b), Blocks.CARPET.purple().defaultBlockState());
			}
		}
		put(level, box, local(0, wall, 0), ModBlocks.PRISM_LAMP.defaultBlockState());

		BlockPos home = local(0, 1, 0);
		if (box.isInside(home)) {
			EndVillager villager = ModEntities.END_VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (villager != null) {
				villager.setProfession(job);
				villager.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
				villager.finalizeSpawn(level, level.getCurrentDifficultyAt(home), EntitySpawnReason.STRUCTURE, null);
				villager.setHomeTo(home, 14);
				villager.setPersistenceRequired();
				level.addFreshEntityWithPassengers(villager);
			}
		}
	}

	private void chest(WorldGenLevel level, BoundingBox box, int a, int h, int b, Direction facing) {
		BlockPos pos = local(a, h, b);
		if (box.isInside(pos)) {
			level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
			RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(seed ^ pos.asLong()), pos, LOOT);
		}
	}
}
