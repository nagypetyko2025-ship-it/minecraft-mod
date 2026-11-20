package hu.endrealms.structure;

import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.entity.VoidSentinel;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Void Bastion: a tall voidstone tower built procedurally. Variants: radius 5-7, 3-6 floors, square or
 * octagonal plan, pointed spire or crenellated crown roof, and randomly furnished floors (chests, tables,
 * bookshelves, carpets, end rod lamps). A spiral staircase around a central pillar runs from the ground up
 * to the roof; every floor has purple stained glass windows. Everything is derived from the stored seed, so
 * each chunk builds its part of the same tower.
 */
public class VoidBastionPiece extends StructurePiece {
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/void_bastion"));
	public static final ResourceKey<LootTable> TREASURE = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/void_bastion_treasure"));

	private static final int FLOOR_HEIGHT = 6;
	private static final int FOUNDATION_DEPTH = 24;
	/** Spiral stair ring around the central pillar, clockwise; each step rises one block. */
	private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

	private final int cx;
	private final int by;
	private final int cz;
	private final int radius;
	private final int floors;
	private final boolean octagon;
	private final boolean spireRoof;
	private final long seed;

	public VoidBastionPiece(RandomSource random, BlockPos base) {
		this(base.getX(), base.getY(), base.getZ(), 5 + random.nextInt(3), 3 + random.nextInt(4),
				random.nextBoolean(), random.nextBoolean(), random.nextLong());
	}

	private VoidBastionPiece(int cx, int by, int cz, int radius, int floors, boolean octagon, boolean spireRoof, long seed) {
		super(ModStructures.VOID_BASTION_PIECE, 0, box(cx, by, cz, radius, floors));
		this.cx = cx;
		this.by = by;
		this.cz = cz;
		this.radius = radius;
		this.floors = floors;
		this.octagon = octagon;
		this.spireRoof = spireRoof;
		this.seed = seed;
	}

	public VoidBastionPiece(CompoundTag tag) {
		super(ModStructures.VOID_BASTION_PIECE, tag);
		this.cx = tag.getIntOr("CX", 0);
		this.by = tag.getIntOr("BY", 64);
		this.cz = tag.getIntOr("CZ", 0);
		this.radius = tag.getIntOr("Radius", 6);
		this.floors = tag.getIntOr("Floors", 4);
		this.octagon = tag.getBooleanOr("Octagon", false);
		this.spireRoof = tag.getBooleanOr("Spire", true);
		this.seed = tag.getLongOr("Seed", 0L);
	}

	private static BoundingBox box(int cx, int by, int cz, int radius, int floors) {
		int top = by + floors * FLOOR_HEIGHT + radius + 4;
		return new BoundingBox(cx - radius - 2, by - FOUNDATION_DEPTH, cz - radius - 2, cx + radius + 2, top, cz + radius + 2);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", cx);
		tag.putInt("BY", by);
		tag.putInt("CZ", cz);
		tag.putInt("Radius", radius);
		tag.putInt("Floors", floors);
		tag.putBoolean("Octagon", octagon);
		tag.putBoolean("Spire", spireRoof);
		tag.putLong("Seed", seed);
	}

	// ---------------------------------------------------------------- shape helpers (dx, dz relative to centre)

	private boolean inShape(int dx, int dz, int r) {
		if (Math.abs(dx) > r || Math.abs(dz) > r) {
			return false;
		}
		return !octagon || Math.abs(dx) + Math.abs(dz) <= r + r / 2;
	}

	private boolean isWall(int dx, int dz) {
		return inShape(dx, dz, radius) && (!inShape(dx + 1, dz, radius) || !inShape(dx - 1, dz, radius)
				|| !inShape(dx, dz + 1, radius) || !inShape(dx, dz - 1, radius));
	}

	private static boolean inStairArea(int dx, int dz) {
		return Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
	}

	private boolean isWindow(int dx, int dz, int yInFloor) {
		if (yInFloor != 2 && yInFloor != 3) {
			return false;
		}
		int span = radius >= 6 ? 1 : 0;
		boolean faceX = Math.abs(dx) == radius && Math.abs(dz) <= span;
		boolean faceZ = Math.abs(dz) == radius && Math.abs(dx) <= span;
		boolean diagonal = octagon && Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1 && isWall(dx, dz)
				&& Math.abs(dx) > 1 && Math.abs(dz) > 1;
		return faceX || faceZ || diagonal;
	}

	private void set(WorldGenLevel level, BoundingBox chunkBox, int dx, int y, int dz, BlockState state) {
		BlockPos pos = new BlockPos(cx + dx, by + y, cz + dz);
		if (chunkBox.isInside(pos)) {
			level.setBlock(pos, state, 2);
		}
	}

	// ---------------------------------------------------------------- building

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unused, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		RandomSource random = RandomSource.create(seed);
		BlockState bricks = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		BlockState polished = ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
		BlockState glass = Blocks.STAINED_GLASS.purple().defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		int totalH = floors * FLOOR_HEIGHT;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (!inShape(dx, dz, radius)) {
					continue;
				}
				boolean wall = isWall(dx, dz);
				// foundation: fill the gap under the tower down to the island
				for (int y = -1; y >= -FOUNDATION_DEPTH; y--) {
					BlockPos pos = new BlockPos(cx + dx, by + y, cz + dz);
					if (!chunkBox.isInside(pos) || !level.getBlockState(pos).canBeReplaced()) {
						break;
					}
					level.setBlock(pos, bricks, 2);
				}
				for (int y = 0; y <= totalH; y++) {
					int yIn = y % FLOOR_HEIGHT;
					BlockState state;
					if (dx == 0 && dz == 0) {
						state = polished;                         // central pillar
					} else if (wall) {
						if (yIn == 0) {
							state = polished;                     // belt course at every floor
						} else if (isWindow(dx, dz, yIn)) {
							state = glass;
						} else {
							state = bricks;
						}
					} else if (yIn == 0) {
						state = (inStairArea(dx, dz) && y > 0) ? air : (y == 0 ? polished : bricks);
					} else {
						state = air;
					}
					set(level, chunkBox, dx, y, dz, state);
				}
			}
		}

		// entrance on the north side, with a step outside
		for (int w = -1; w <= 1; w++) {
			for (int y = 1; y <= 3; y++) {
				set(level, chunkBox, w, y, -radius, (y == 3 && w != 0) ? bricks : air);
			}
		}
		set(level, chunkBox, 0, 0, -radius - 1, ModBlocks.VOIDSTONE_BRICK_STAIRS.defaultBlockState()
				.setValue(StairBlock.FACING, Direction.SOUTH));

		// spiral staircase around the pillar, up to the roof
		for (int i = 0; i < totalH; i++) {
			int[] cell = RING[i % RING.length];
			int[] next = RING[(i + 1) % RING.length];
			Direction facing = Direction.getApproximateNearest(next[0] - cell[0], 0, next[1] - cell[1]);
			set(level, chunkBox, cell[0], 1 + i, cell[1],
					ModBlocks.VOIDSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
		}

		for (int floor = 0; floor < floors; floor++) {
			furnish(level, chunkBox, random, floor, floor == floors - 1);
		}

		// guards: one on the ground floor, one on every middle floor, two by the treasure on the top floor
		int g = Math.max(2, radius - 3);
		for (int floor = 0; floor < floors; floor++) {
			int y = floor * FLOOR_HEIGHT + 1;
			guard(level, chunkBox, -g, y, g);
			if (floor == floors - 1) {
				guard(level, chunkBox, g, y, -g);
			}
		}

		if (spireRoof) {
			buildSpire(level, chunkBox, totalH);
		} else {
			buildCrown(level, chunkBox, random, totalH);
		}
	}

	private void furnish(WorldGenLevel level, BoundingBox box, RandomSource random, int floor, boolean top) {
		int y = floor * FLOOR_HEIGHT + 1;          // standing level
		int ceiling = (floor + 1) * FLOOR_HEIGHT - 1;
		int in = radius - 1;                        // just inside the wall
		int lamp = radius - 2;

		// end rod lamps hanging from the ceiling
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				if (inShape(sx * lamp, sz * lamp, radius) && !isWall(sx * lamp, sz * lamp)) {
					set(level, box, sx * lamp, ceiling, sz * lamp, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN));
				}
			}
		}

		// chests against the east / west walls, facing the room; the top floor gets the treasure chest
		int chests = top ? 2 : (floor == 0 ? 1 : random.nextInt(3));
		if (chests >= 1) {
			chest(level, box, random, in, y, 0, Direction.WEST, top ? TREASURE : LOOT);
		}
		if (chests >= 2) {
			chest(level, box, random, -in, y, 0, Direction.EAST, LOOT);
		}

		// one themed corner per floor
		switch (random.nextInt(4)) {
			case 0 -> { // library: bookshelves along the south wall
				for (int w = -2; w <= 2; w++) {
					if (Math.abs(w) <= 1 && radius - 1 <= 2) {
						continue;
					}
					for (int h = 0; h < 2; h++) {
						set(level, box, w, y + h, in, Blocks.BOOKSHELF.defaultBlockState());
					}
				}
			}
			case 1 -> { // table with brewing stand and two stools
				set(level, box, 0, y, in - 1, ModBlocks.POLISHED_VOIDSTONE.defaultBlockState());
				set(level, box, 0, y + 1, in - 1, Blocks.BREWING_STAND.defaultBlockState());
				set(level, box, -1, y, in - 1, ModBlocks.POLISHED_VOIDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
				set(level, box, 1, y, in - 1, ModBlocks.POLISHED_VOIDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
			}
			case 2 -> { // dining table of astral wood
				for (int w = -1; w <= 1; w++) {
					set(level, box, w, y, in - 1, ModBlocks.ASTRAL_FENCE.defaultBlockState());
					set(level, box, w, y + 1, in - 1, ModBlocks.POLISHED_VOIDSTONE_SLAB.defaultBlockState());
				}
			}
			default -> { // purple carpet over the floor
				for (int dx = -in; dx <= in; dx++) {
					for (int dz = -in; dz <= in; dz++) {
						if (inShape(dx, dz, radius) && !isWall(dx, dz) && !inStairArea(dx, dz) && random.nextFloat() < 0.7F) {
							BlockPos pos = new BlockPos(cx + dx, by + y, cz + dz);
							if (box.isInside(pos) && level.getBlockState(pos).isAir()) {
								level.setBlock(pos, Blocks.CARPET.purple().defaultBlockState(), 2);
							}
						}
					}
				}
			}
		}
	}

	/** Places a persistent Void Sentinel standing guard (only from the chunk that contains its position). */
	private void guard(WorldGenLevel level, BoundingBox box, int dx, int y, int dz) {
		BlockPos pos = new BlockPos(cx + dx, by + y, cz + dz);
		if (!box.isInside(pos)) {
			return;
		}
		VoidSentinel sentinel = ModEntities.VOID_SENTINEL.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (sentinel == null) {
			return;
		}
		sentinel.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		sentinel.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		sentinel.setPersistenceRequired();
		level.addFreshEntityWithPassengers(sentinel);
	}

	private void chest(WorldGenLevel level, BoundingBox box, RandomSource random, int dx, int y, int dz, Direction facing,
			ResourceKey<LootTable> loot) {
		BlockPos pos = new BlockPos(cx + dx, by + y, cz + dz);
		if (box.isInside(pos)) {
			level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
			// own random per chest: the shared sequence must not depend on which chunk is being built
			RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(seed ^ pos.asLong()), pos, loot);
		}
	}

	/** Hollow stepped spire narrowing to a point with an end rod on top. */
	private void buildSpire(WorldGenLevel level, BoundingBox box, int totalH) {
		BlockState bricks = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		for (int layer = 1; layer <= radius; layer++) {
			int r = radius - layer + 1;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (!inShape(dx, dz, r)) {
						continue;
					}
					boolean edge = !inShape(dx + 1, dz, r) || !inShape(dx - 1, dz, r) || !inShape(dx, dz + 1, r) || !inShape(dx, dz - 1, r);
					boolean window = r >= 3 && edge && (dx == 0 || dz == 0) && layer % 2 == 0;
					set(level, box, dx, totalH + layer, dz, edge ? (window ? Blocks.STAINED_GLASS.purple().defaultBlockState() : bricks)
							: Blocks.AIR.defaultBlockState());
				}
			}
		}
		set(level, box, 0, totalH + radius + 1, 0, ModBlocks.POLISHED_VOIDSTONE.defaultBlockState());
		set(level, box, 0, totalH + radius + 2, 0, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP));
		// top floor of the spire: an end rod inside to light it
		set(level, box, 2, totalH + 1, 2, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP));
	}

	/** Open roof terrace with crenellations, corner towers topped with end rods. */
	private void buildCrown(WorldGenLevel level, BoundingBox box, RandomSource random, int totalH) {
		BlockState wall = ModBlocks.VOIDSTONE_BRICK_WALL.defaultBlockState();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (!isWall(dx, dz)) {
					continue;
				}
				set(level, box, dx, totalH + 1, dz, ModBlocks.VOIDSTONE_BRICKS.defaultBlockState());
				if ((dx + dz) % 2 == 0) {
					set(level, box, dx, totalH + 2, dz, wall);
				}
			}
		}
		// corner towers on the outermost diagonal wall cells
		int corner = radius;
		while (corner > 1 && !isWall(corner, corner)) {
			corner--;
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				int px = sx * corner;
				int pz = sz * corner;
				for (int h = 1; h <= 4; h++) {
					set(level, box, px, totalH + h, pz, ModBlocks.POLISHED_VOIDSTONE.defaultBlockState());
				}
				set(level, box, px, totalH + 5, pz, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP));
			}
		}
		// roof garden: a treasure-less lookout with a chest and a lamp on the pillar
		set(level, box, 0, totalH + 1, 0, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP));
		chest(level, box, random, radius - 2, totalH + 1, radius - 2, Direction.NORTH, LOOT);
	}
}
