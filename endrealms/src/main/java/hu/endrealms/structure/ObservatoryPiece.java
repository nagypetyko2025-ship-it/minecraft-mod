package hu.endrealms.structure;

import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModStructures;
import hu.endrealms.worldgen.BasinSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Observatory: a half-ruined tower with a telescope, which is only ever decoration. Variants: round or octagonal, one to
 * three storeys, with or without a sunken library, a whole dome, a burst dome, an open roof terrace or no roof at all
 * with the telescope fallen to the floor, and sometimes a side annex.
 */
public class ObservatoryPiece extends StructurePiece {
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/observatory"));
	private static final int FLOOR_HEIGHT = 6;
	private static final int BASEMENT = 6;
	private static final int DOME = 0;
	private static final int BURST_DOME = 1;
	private static final int TERRACE = 2;
	private static final int ROOFLESS = 3;
	private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}};

	private final int cx;
	private final int by;
	private final int cz;
	private final int radius;
	private final int floors;
	private final boolean octagon;
	private final boolean basement;
	private final int roof;
	private final int annex;
	private final float aim;
	private final long seed;

	public ObservatoryPiece(RandomSource random, int x, int y, int z) {
		this(x, y, z, 6 + random.nextInt(4), 1 + random.nextInt(3), random.nextBoolean(), random.nextFloat() < 0.65F, random.nextInt(4),
				random.nextInt(5) - 2, random.nextFloat() * Mth.TWO_PI, random.nextLong());
	}

	private ObservatoryPiece(int x, int y, int z, int radius, int floors, boolean octagon, boolean basement, int roof, int annex, float aim, long seed) {
		super(ModStructures.OBSERVATORY_PIECE, 0, new BoundingBox(x - radius - 12, y - 32, z - radius - 12, x + radius + 12, y + floors * FLOOR_HEIGHT + radius + 8, z + radius + 12));
		this.cx = x;
		this.by = y;
		this.cz = z;
		this.radius = radius;
		this.floors = floors;
		this.octagon = octagon;
		this.basement = basement;
		this.roof = roof;
		this.annex = annex;
		this.aim = aim;
		this.seed = seed;
	}

	public ObservatoryPiece(CompoundTag tag) {
		super(ModStructures.OBSERVATORY_PIECE, tag);
		this.cx = tag.getIntOr("CX", 0);
		this.by = tag.getIntOr("BY", 64);
		this.cz = tag.getIntOr("CZ", 0);
		this.radius = tag.getIntOr("Radius", 7);
		this.floors = tag.getIntOr("Floors", 2);
		this.octagon = tag.getBooleanOr("Octagon", false);
		this.basement = tag.getBooleanOr("Basement", true);
		this.roof = tag.getIntOr("Roof", 0);
		this.annex = tag.getIntOr("Annex", -1);
		this.aim = tag.getFloatOr("Aim", 0.0F);
		this.seed = tag.getLongOr("Seed", 0L);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", cx);
		tag.putInt("BY", by);
		tag.putInt("CZ", cz);
		tag.putInt("Radius", radius);
		tag.putInt("Floors", floors);
		tag.putBoolean("Octagon", octagon);
		tag.putBoolean("Basement", basement);
		tag.putInt("Roof", roof);
		tag.putInt("Annex", annex);
		tag.putFloat("Aim", aim);
		tag.putLong("Seed", seed);
	}

	private boolean inside(int dx, int dz, int r) {
		if (octagon) {
			return Math.abs(dx) <= r && Math.abs(dz) <= r && Math.abs(dx) + Math.abs(dz) <= r + r / 2;
		}
		return dx * dx + dz * dz <= r * r + r / 2;
	}

	private boolean wall(int dx, int dz) {
		return inside(dx, dz, radius) && !(inside(dx + 1, dz, radius) && inside(dx - 1, dz, radius) && inside(dx, dz + 1, radius) && inside(dx, dz - 1, radius));
	}

	private boolean inShaft(int dx, int dz) {
		return Math.abs(dx - (radius - 3)) <= 1 && Math.abs(dz) <= 1;
	}

	private void set(WorldGenLevel level, BoundingBox box, int dx, int y, int dz, BlockState state) {
		BlockPos pos = new BlockPos(cx + dx, by + y, cz + dz);
		if (box.isInside(pos)) {
			level.setBlock(pos, state, 2);
		}
	}

	private int top() {
		return floors * FLOOR_HEIGHT;
	}

	private int bottom() {
		return basement ? -BASEMENT : 0;
	}

	/** Upper walls fall away where this is true; roofless towers lose much more of them. */
	private boolean ruined(int dx, int y, int dz) {
		if (y < 2) {
			return false;
		}
		int around = Math.round((float) Mth.atan2(dz, dx) * radius);
		float height = y / (float) top();
		float limit = (roof == ROOFLESS ? 0.15F : 0.75F) - height * 0.45F;
		return BasinSites.noise(around + 700, y, 4, 71 + (int) (seed & 7)) > limit;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unused, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		RandomSource random = RandomSource.create(seed);
		buildTower(level, box);
		buildStairs(level, box);
		buildEntrance(level, box);
		if (annex >= 0) {
			buildAnnex(level, box, random);
		}
		furnishLibrary(level, box, random, bottom());
		for (int floor = basement ? 0 : 1; floor < floors; floor++) {
			furnishStudy(level, box, random, floor * FLOOR_HEIGHT);
		}
		if (roof == DOME || roof == BURST_DOME) {
			buildDome(level, box, random);
		}
		boolean onTerrace = roof == TERRACE;
		buildTelescope(level, box, onTerrace ? top() : (floors - 1) * FLOOR_HEIGHT);
	}

	private void buildTower(WorldGenLevel level, BoundingBox box) {
		BlockState bricks = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		BlockState polished = ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
		BlockState glass = Blocks.STAINED_GLASS.cyan().defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		int topY = top();
		boolean closedTop = roof == TERRACE;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (!inside(dx, dz, radius)) {
					continue;
				}
				boolean isWall = wall(dx, dz);
				for (int y = bottom() - 1; y >= bottom() - 24; y--) {
					BlockPos pos = new BlockPos(cx + dx, by + y, cz + dz);
					if (!box.isInside(pos) || !level.getBlockState(pos).canBeReplaced()) {
						break;
					}
					level.setBlock(pos, bricks, 2);
				}
				for (int y = bottom(); y <= topY; y++) {
					int yIn = Math.floorMod(y, FLOOR_HEIGHT);
					BlockState state;
					if (isWall) {
						boolean window = yIn >= 2 && yIn <= 3 && y > 0 && (dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz));
						state = ruined(dx, y, dz) ? air : window ? glass : yIn == 0 ? polished : bricks;
					} else if (yIn == 0) {
						boolean opening = (inShaft(dx, dz) && y > bottom() && (y < topY || closedTop)) || (y == topY && !closedTop);
						state = opening ? air : polished;
					} else {
						state = air;
					}
					set(level, box, dx, y, dz, state);
				}
				for (int y = topY + 1; y <= topY + radius + 4; y++) {
					set(level, box, dx, y, dz, air);
				}
			}
		}
		if (closedTop) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (wall(dx, dz) && (dx + dz) % 2 == 0) {
						set(level, box, dx, topY + 1, dz, ModBlocks.VOIDSTONE_BRICK_WALL.defaultBlockState());
					}
				}
			}
		}
	}

	private void buildStairs(WorldGenLevel level, BoundingBox box) {
		int sx = radius - 3;
		int last = roof == TERRACE ? top() : (floors - 1) * FLOOR_HEIGHT;
		BlockState pillar = ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
		for (int y = bottom() + 1; y <= last; y++) {
			set(level, box, sx, y, 0, pillar);
		}
		int steps = last - bottom();
		for (int i = 0; i < steps; i++) {
			int[] cell = RING[i % RING.length];
			int[] next = RING[(i + 1) % RING.length];
			Direction facing = Direction.getApproximateNearest(next[0] - cell[0], 0, next[1] - cell[1]);
			set(level, box, sx + cell[0], bottom() + 1 + i, cell[1], ModBlocks.VOIDSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
		}
	}

	private void buildEntrance(WorldGenLevel level, BoundingBox box) {
		int edge = -radius;
		while (edge < 0 && !inside(0, edge, radius)) {
			edge++;
		}
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int w = -1; w <= 1; w++) {
			for (int depth = 0; depth < 2; depth++) {
				for (int y = 1; y <= 3; y++) {
					set(level, box, w, y, edge + depth, air);
				}
			}
		}
	}

	private void buildAnnex(WorldGenLevel level, BoundingBox box, RandomSource random) {
		int[] side = SIDES[annex];
		int half = 4;
		int ax = side[0] * (radius + half);
		int az = side[1] * (radius + half);
		BlockState bricks = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		BlockState polished = ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
				for (int y = -1; y >= -24; y--) {
					BlockPos pos = new BlockPos(cx + ax + dx, by + y, cz + az + dz);
					if (!box.isInside(pos) || !level.getBlockState(pos).canBeReplaced()) {
						break;
					}
					level.setBlock(pos, bricks, 2);
				}
				set(level, box, ax + dx, 0, az + dz, polished);
				for (int y = 1; y <= 4; y++) {
					boolean gone = y >= 3 && BasinSites.noise(cx + ax + dx * 3, cz + az + dz * 3 + y, 3, 83) > 0.3F;
					set(level, box, ax + dx, y, az + dz, edge && !gone ? bricks : air);
				}
				set(level, box, ax + dx, 5, az + dz, BasinSites.noise(cx + dx * 2, cz + dz * 2, 4, 87) > 0.2F ? air : ModBlocks.POLISHED_VOIDSTONE_SLAB.defaultBlockState());
			}
		}
		for (int step = 0; step <= radius; step++) {
			int dx = side[0] * (radius + half - step);
			int dz = side[1] * (radius + half - step);
			if (Math.abs(dx) >= radius - 2 || Math.abs(dz) >= radius - 2) {
				for (int y = 1; y <= 2; y++) {
					set(level, box, dx, y, dz, air);
				}
			}
		}
		for (int i = -2; i <= 2; i += 2) {
			set(level, box, ax + i, 1, az + (side[1] == 0 ? half - 1 : side[1] * (half - 1)), Blocks.BOOKSHELF.defaultBlockState());
		}
		chest(level, box, ax, 1, az);
	}

	/** Shelves round the wall, a map table and the best chests. */
	private void furnishLibrary(WorldGenLevel level, BoundingBox box, RandomSource random, int floorY) {
		BlockState shelf = Blocks.BOOKSHELF.defaultBlockState();
		for (int dx = -radius + 1; dx < radius; dx++) {
			for (int dz = -radius + 1; dz < radius; dz++) {
				boolean atWall = inside(dx, dz, radius - 1) && !wall(dx, dz) && !inside(dx, dz, radius - 2);
				if (atWall && !inShaft(dx, dz) && !(floorY == 0 && dz < 0 && Math.abs(dx) <= 2) && random.nextFloat() < 0.8F) {
					for (int h = 1; h <= 3; h++) {
						set(level, box, dx, floorY + h, dz, shelf);
					}
				}
			}
		}
		set(level, box, -1, floorY + 1, 0, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
		set(level, box, 1, floorY + 1, -1, Blocks.LECTERN.defaultBlockState());
		chest(level, box, -2, floorY + 1, 2);
		chest(level, box, 1, floorY + 1, 2);
		for (int i = -1; i <= 1; i += 2) {
			set(level, box, i * 2, floorY + FLOOR_HEIGHT - 1, i * 2, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN));
		}
	}

	private void furnishStudy(WorldGenLevel level, BoundingBox box, RandomSource random, int floorY) {
		for (int dx = -2; dx <= 2; dx++) {
			if (random.nextFloat() < 0.7F) {
				set(level, box, dx, floorY + 1, -3, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
			}
		}
		set(level, box, -3, floorY + 1, 0, Blocks.BOOKSHELF.defaultBlockState());
		set(level, box, -3, floorY + 2, 0, Blocks.BOOKSHELF.defaultBlockState());
		set(level, box, 0, floorY + FLOOR_HEIGHT - 1, 0, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN));
		if (random.nextFloat() < 0.6F) {
			chest(level, box, -2, floorY + 1, 3);
		}
	}

	private void buildDome(WorldGenLevel level, BoundingBox box, RandomSource random) {
		BlockState rib = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		BlockState pane = Blocks.STAINED_GLASS.cyan().defaultBlockState();
		for (int h = 1; h <= radius; h++) {
			int outer = Math.round(Mth.sqrt(radius * radius - h * h + radius / 2.0F));
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int r2 = dx * dx + dz * dz;
					if (r2 > outer * outer + outer / 2 || r2 < (outer - 1) * (outer - 1)) {
						continue;
					}
					if (roof == BURST_DOME && (dx * Mth.cos(aim) + dz * Mth.sin(aim)) > -1.0F && random.nextFloat() < 0.8F) {
						continue;
					}
					boolean isRib = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
					set(level, box, dx, top() + h, dz, isRib ? rib : pane);
				}
			}
		}
	}

	/** Pedestal and tube with a crystal lens; on a roofless tower it has come down and lies across the floor. */
	private void buildTelescope(WorldGenLevel level, BoundingBox box, int floorY) {
		BlockState body = ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
		BlockState lens = ModBlocks.PRISM_CRYSTAL_BLOCK.defaultBlockState();
		BlockState tip = ModBlocks.PRISM_LAMP.defaultBlockState();
		boolean fallen = roof == ROOFLESS;
		int length = radius - 1;
		for (int y = 1; y <= (fallen ? 1 : 2); y++) {
			set(level, box, 0, floorY + y, 0, ModBlocks.POLISHED_VOIDSTONE_WALL.defaultBlockState());
		}
		for (int t = -2; t <= length; t++) {
			int dx = Math.round(Mth.cos(aim) * t);
			int dz = Math.round(Mth.sin(aim) * t);
			int y = fallen ? 1 : 3 + Math.round(t * 0.7F);
			set(level, box, dx, floorY + y, dz, t == length ? tip : t >= length - 2 ? lens : body);
		}
	}

	private void chest(WorldGenLevel level, BoundingBox box, int dx, int y, int dz) {
		BlockPos pos = new BlockPos(cx + dx, by + y, cz + dz);
		if (box.isInside(pos)) {
			level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
			RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(seed ^ pos.asLong()), pos, LOOT);
		}
	}
}
