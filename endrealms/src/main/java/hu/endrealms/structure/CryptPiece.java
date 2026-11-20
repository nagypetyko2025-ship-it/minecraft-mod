package hu.endrealms.structure;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.AshWraith;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModStructures;
import hu.endrealms.worldgen.BasinSites;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Crypt: a building on the surface in one of four styles (mausoleum, domed tomb, roofless ruin, obelisk court) with a
 * spiral stair down to vaults laid out on a grid. The rooms are burial crypts, storerooms, ossuaries, vented trap halls
 * and, furthest from the stairs, the burial chamber. The whole layout comes from the seed, so every chunk agrees on it.
 */
public class CryptPiece extends StructurePiece {
	private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/crypt"));
	private static final ResourceKey<LootTable> TREASURE = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/crypt_treasure"));
	private static final int CELL = 12;
	private static final int ROOM = 5;           // walls stand this far from a room's centre
	private static final int ROOM_HEIGHT = 5;    // ceiling this far above the floor
	private static final int GRID = 2;
	private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
	private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private enum Type { ENTRY, CRYPT, STORAGE, OSSUARY, TRAP_HALL, BURIAL }

	private record Room(int gx, int gz, int depth, Type type) {
	}

	private record Link(int ax, int az, int bx, int bz) {
	}

	private final int cx;
	private final int gy;
	private final int floorY;
	private final int cz;
	private final int style;
	private final int facing;
	private final long seed;
	private final List<Room> rooms = new ArrayList<>();
	private final List<Link> links = new ArrayList<>();

	public CryptPiece(RandomSource random, int x, int ground, int floor, int z) {
		this(x, ground, floor, z, random.nextInt(4), random.nextInt(4), random.nextLong());
	}

	private CryptPiece(int x, int ground, int floor, int z, int style, int facing, long seed) {
		super(ModStructures.CRYPT_PIECE, 0, new BoundingBox(x - 32, floor - 2, z - 32, x + 32, ground + 13, z + 32));
		this.cx = x;
		this.gy = ground;
		this.floorY = floor;
		this.cz = z;
		this.style = style;
		this.facing = facing;
		this.seed = seed;
		layOut();
	}

	public CryptPiece(CompoundTag tag) {
		super(ModStructures.CRYPT_PIECE, tag);
		this.cx = tag.getIntOr("CX", 0);
		this.gy = tag.getIntOr("GY", 64);
		this.floorY = tag.getIntOr("FloorY", 40);
		this.cz = tag.getIntOr("CZ", 0);
		this.style = tag.getIntOr("Style", 0);
		this.facing = tag.getIntOr("Facing", 0);
		this.seed = tag.getLongOr("Seed", 0L);
		layOut();
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", cx);
		tag.putInt("GY", gy);
		tag.putInt("FloorY", floorY);
		tag.putInt("CZ", cz);
		tag.putInt("Style", style);
		tag.putInt("Facing", facing);
		tag.putLong("Seed", seed);
	}

	private void layOut() {
		RandomSource random = RandomSource.create(seed ^ 0xC0FFEEL);
		List<int[]> cells = new ArrayList<>();          // {gx, gz, depth}
		cells.add(new int[] {0, 0, 0});
		int wanted = 6 + random.nextInt(5);
		for (int tries = 0; tries < 200 && cells.size() < wanted; tries++) {
			int[] from = cells.get(random.nextInt(cells.size()));
			int[] step = STEPS[random.nextInt(STEPS.length)];
			int gx = from[0] + step[0];
			int gz = from[1] + step[1];
			if (Math.abs(gx) <= GRID && Math.abs(gz) <= GRID && cells.stream().noneMatch(c -> c[0] == gx && c[1] == gz)) {
				cells.add(new int[] {gx, gz, from[2] + 1});
				links.add(new Link(from[0], from[1], gx, gz));
			}
		}
		int deepest = 1;
		for (int i = 1; i < cells.size(); i++) {
			if (cells.get(i)[2] >= cells.get(deepest)[2]) {
				deepest = i;
			}
		}
		Type[] mix = {Type.CRYPT, Type.STORAGE, Type.OSSUARY, Type.TRAP_HALL};
		int next = random.nextInt(mix.length);
		for (int i = 0; i < cells.size(); i++) {
			int[] c = cells.get(i);
			Type type = i == 0 ? Type.ENTRY : i == deepest ? Type.BURIAL : mix[next++ % mix.length];
			rooms.add(new Room(c[0], c[1], c[2], type));
		}
	}

	// ---------------------------------------------------------------- helpers

	private static void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);
		if (box.isInside(pos)) {
			level.setBlock(pos, state, 2);
		}
	}

	/** Places a block relative to the shaft in the building's own rotation. */
	private void local(WorldGenLevel level, BoundingBox box, int dx, int y, int dz, BlockState state) {
		int rx = switch (facing) {
			case 1 -> -dz;
			case 2 -> -dx;
			case 3 -> dz;
			default -> dx;
		};
		int rz = switch (facing) {
			case 1 -> dx;
			case 2 -> -dz;
			case 3 -> -dx;
			default -> dz;
		};
		put(level, box, cx + rx, gy + y, cz + rz, state);
	}

	private static BlockState b(net.minecraft.world.level.block.Block block) {
		return block.defaultBlockState();
	}

	private void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, boolean treasure) {
		BlockPos pos = new BlockPos(x, y, z);
		if (box.isInside(pos)) {
			level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
			RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(seed ^ pos.asLong()), pos, treasure ? TREASURE : LOOT);
		}
	}

	private void vent(WorldGenLevel level, BoundingBox box, int x, int y, int z, RandomSource random) {
		BlockPos pos = new BlockPos(x, y, z);
		if (box.isInside(pos)) {
			level.setBlock(pos, b(ModBlocks.VOLCANIC_VENT), 2);
			level.scheduleTick(pos, ModBlocks.VOLCANIC_VENT, 20 + random.nextInt(60));
		}
	}

	private void guard(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		if (!box.isInside(pos)) {
			return;
		}
		AshWraith wraith = ModEntities.ASH_WRAITH.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (wraith != null) {
			wraith.snapTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
			wraith.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
			wraith.setPersistenceRequired();
			level.addFreshEntityWithPassengers(wraith);
		}
	}

	// ---------------------------------------------------------------- building

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unused, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		for (Link link : links) {
			buildCorridor(level, box, link);
		}
		for (Room room : rooms) {
			buildRoom(level, box, room);
		}
		for (Link link : links) {
			openDoorway(level, box, link);
		}
		for (Room room : rooms) {
			furnish(level, box, room, RandomSource.create(seed + room.gx() * 341873128712L + room.gz() * 132897987541L));
		}
		buildShaft(level, box);
		switch (style) {
			case 1 -> buildDomedTomb(level, box);
			case 2 -> buildRuin(level, box);
			case 3 -> buildObeliskCourt(level, box);
			default -> buildMausoleum(level, box);
		}
		openTop(level, box);
	}

	private void buildRoom(WorldGenLevel level, BoundingBox box, Room room) {
		int rx = cx + room.gx() * CELL;
		int rz = cz + room.gz() * CELL;
		BlockState bricks = b(ModBlocks.VOIDSTONE_BRICKS);
		BlockState scorched = b(ModBlocks.SCORCHED_END_STONE);
		BlockState polished = b(ModBlocks.POLISHED_VOIDSTONE);
		BlockState air = b(Blocks.AIR);
		for (int dx = -ROOM; dx <= ROOM; dx++) {
			for (int dz = -ROOM; dz <= ROOM; dz++) {
				boolean edge = Math.abs(dx) == ROOM || Math.abs(dz) == ROOM;
				for (int y = 0; y <= ROOM_HEIGHT; y++) {
					BlockState state;
					if (y == 0) {
						state = (dx + dz) % 2 == 0 ? polished : scorched;
					} else if (edge || y == ROOM_HEIGHT) {
						state = BasinSites.noise(rx + dx * 3, rz + dz * 3 + y, 3, 91) > 0.35F ? scorched : bricks;
					} else {
						state = air;
					}
					put(level, box, rx + dx, floorY + y, rz + dz, state);
				}
			}
		}
	}

	private void buildCorridor(WorldGenLevel level, BoundingBox box, Link link) {
		int ax = cx + link.ax() * CELL;
		int az = cz + link.az() * CELL;
		int stepX = link.bx() - link.ax();
		int stepZ = link.bz() - link.az();
		BlockState bricks = b(ModBlocks.VOIDSTONE_BRICKS);
		for (int along = ROOM; along <= CELL - ROOM; along++) {
			for (int across = -2; across <= 2; across++) {
				for (int y = 0; y <= 4; y++) {
					int x = ax + stepX * along + stepZ * across;
					int z = az + stepZ * along + stepX * across;
					put(level, box, x, floorY + y, z, bricks);
				}
			}
		}
	}

	private void openDoorway(WorldGenLevel level, BoundingBox box, Link link) {
		int ax = cx + link.ax() * CELL;
		int az = cz + link.az() * CELL;
		int stepX = link.bx() - link.ax();
		int stepZ = link.bz() - link.az();
		for (int along = ROOM; along <= CELL - ROOM; along++) {
			for (int across = -1; across <= 1; across++) {
				for (int y = 1; y <= 3; y++) {
					put(level, box, ax + stepX * along + stepZ * across, floorY + y, az + stepZ * along + stepX * across, b(Blocks.AIR));
				}
			}
		}
	}

	private void sarcophagus(WorldGenLevel level, BoundingBox box, int x, int y, int z, boolean alongX) {
		for (int i = -1; i <= 1; i++) {
			int px = alongX ? x + i : x;
			int pz = alongX ? z : z + i;
			put(level, box, px, y, pz, b(ModBlocks.POLISHED_VOIDSTONE));
			put(level, box, px, y + 1, pz, b(ModBlocks.POLISHED_VOIDSTONE_SLAB));
		}
	}

	private void furnish(WorldGenLevel level, BoundingBox box, Room room, RandomSource random) {
		int rx = cx + room.gx() * CELL;
		int rz = cz + room.gz() * CELL;
		int y = floorY + 1;
		BlockState lantern = b(Blocks.SOUL_LANTERN);
		switch (room.type()) {
			case ENTRY -> {
				for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
					put(level, box, rx + c[0], y, rz + c[1], lantern);
				}
			}
			case CRYPT -> {
				for (int i = -3; i <= 3; i += 3) {
					sarcophagus(level, box, rx + i, y, rz - 3, false);
					sarcophagus(level, box, rx + i, y, rz + 3, false);
				}
				put(level, box, rx, y, rz, lantern);
				if (random.nextFloat() < 0.5F) {
					chest(level, box, rx + 2, y, rz, false);
				}
				guard(level, box, rx - 2, y, rz);
			}
			case STORAGE -> {
				for (int i = -4; i <= 4; i++) {
					for (int side = -4; side <= 4; side += 8) {
						if (random.nextFloat() < 0.65F) {
							put(level, box, rx + i, y, rz + side, b(Blocks.BARREL));
							if (random.nextFloat() < 0.4F) {
								put(level, box, rx + i, y + 1, rz + side, b(Blocks.BARREL));
							}
						}
					}
				}
				chest(level, box, rx - 1, y, rz, false);
				chest(level, box, rx + 1, y, rz, false);
				put(level, box, rx, y + 3, rz, b(Blocks.END_ROD));
			}
			case OSSUARY -> {
				for (int dx = -4; dx <= 4; dx++) {
					for (int dz = -4; dz <= 4; dz++) {
						boolean atWall = Math.abs(dx) == 4 || Math.abs(dz) == 4;
						if (atWall && random.nextFloat() < 0.75F) {
							int tall = 1 + random.nextInt(3);
							for (int h = 0; h < tall; h++) {
								put(level, box, rx + dx, y + h, rz + dz, b(Blocks.BONE_BLOCK));
							}
							if (random.nextFloat() < 0.35F) {
								put(level, box, rx + dx, y + tall, rz + dz, b(Blocks.SKELETON_SKULL));
							}
						}
					}
				}
				chest(level, box, rx, y, rz, false);
				put(level, box, rx + 1, y, rz + 1, lantern);
				guard(level, box, rx - 2, y, rz - 2);
			}
			case TRAP_HALL -> {
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = -3; dz <= 3; dz++) {
						if ((dx + dz) % 3 == 0 && !(Math.abs(dx) <= 1 && Math.abs(dz) <= 1)) {
							vent(level, box, rx + dx, floorY, rz + dz, random);
						}
					}
				}
				chest(level, box, rx, y, rz, random.nextBoolean());
			}
			case BURIAL -> {
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						put(level, box, rx + dx, y, rz + dz, b(ModBlocks.CRYSTALLINE_VOIDSTONE));
					}
				}
				sarcophagus(level, box, rx, y + 1, rz, true);
				chest(level, box, rx + 3, y, rz, true);
				chest(level, box, rx - 3, y, rz, true);
				for (int[] c : new int[][] {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
					vent(level, box, rx + c[0], floorY, rz + c[1], random);
				}
				put(level, box, rx, y, rz - 3, lantern);
				put(level, box, rx, y, rz + 3, lantern);
				guard(level, box, rx - 2, y, rz - 3);
				guard(level, box, rx + 2, y, rz + 3);
			}
		}
	}

	private void buildShaft(WorldGenLevel level, BoundingBox box) {
		BlockState bricks = b(ModBlocks.VOIDSTONE_BRICKS);
		BlockState pillar = b(ModBlocks.POLISHED_VOIDSTONE);
		BlockState air = b(Blocks.AIR);
		for (int y = floorY + ROOM_HEIGHT; y < gy; y++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean shell = Math.abs(dx) == 2 || Math.abs(dz) == 2;
					put(level, box, cx + dx, y, cz + dz, shell ? bricks : air);
				}
			}
		}
		for (int y = floorY + 1; y < gy; y++) {
			put(level, box, cx, y, cz, pillar);
		}
		int steps = gy - floorY;
		for (int i = 0; i < steps; i++) {
			int[] cell = RING[i % RING.length];
			int[] next = RING[(i + 1) % RING.length];
			Direction up = Direction.getApproximateNearest(next[0] - cell[0], 0, next[1] - cell[1]);
			put(level, box, cx + cell[0], floorY + 1 + i, cz + cell[1], ModBlocks.VOIDSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, up));
		}
	}

	/** Foundation and floor shared by the building styles. */
	private void groundPlate(WorldGenLevel level, BoundingBox box, int reach) {
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				for (int y = gy - 1; y >= gy - 8; y--) {
					BlockPos pos = new BlockPos(cx + dx, y, cz + dz);
					if (Math.max(Math.abs(dx), Math.abs(dz)) > 2 && box.isInside(pos) && level.getBlockState(pos).canBeReplaced()) {
						level.setBlock(pos, b(ModBlocks.VOIDSTONE_BRICKS), 2);
					}
				}
				put(level, box, cx + dx, gy, cz + dz, (dx + dz) % 2 == 0 ? b(ModBlocks.POLISHED_VOIDSTONE) : b(ModBlocks.SCORCHED_END_STONE));
				for (int y = 1; y <= 12; y++) {
					put(level, box, cx + dx, gy + y, cz + dz, b(Blocks.AIR));
				}
			}
		}
	}

	private void buildMausoleum(WorldGenLevel level, BoundingBox box) {
		groundPlate(level, box, 6);
		BlockState bricks = b(ModBlocks.VOIDSTONE_BRICKS);
		BlockState polished = b(ModBlocks.POLISHED_VOIDSTONE);
		BlockState slab = b(ModBlocks.POLISHED_VOIDSTONE_SLAB);
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				boolean edge = Math.abs(dx) == 5 || Math.abs(dz) == 5;
				if (edge) {
					for (int y = 1; y <= 4; y++) {
						boolean door = dz == -5 && Math.abs(dx) <= 1 && y <= 3;
						boolean corner = Math.abs(dx) == 5 && Math.abs(dz) == 5;
						if (!door) {
							local(level, box, dx, y, dz, corner ? polished : bricks);
						}
					}
				}
				local(level, box, dx, 5, dz, bricks);
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				if (ring <= 3) {
					local(level, box, dx, 6, dz, polished);
				}
				if (ring <= 1) {
					local(level, box, dx, 7, dz, slab);
				}
			}
		}
		for (int[] c : new int[][] {{-3, 0}, {3, 0}}) {
			local(level, box, c[0], 1, c[1], b(Blocks.SOUL_LANTERN));
		}
	}

	private void buildDomedTomb(WorldGenLevel level, BoundingBox box) {
		groundPlate(level, box, 7);
		BlockState bricks = b(ModBlocks.VOIDSTONE_BRICKS);
		BlockState scorched = b(ModBlocks.SCORCHED_END_STONE);
		int radius = 6;
		for (int h = 1; h <= radius; h++) {
			int outer = Math.round(Mth.sqrt(radius * radius - h * h / 1.6F));
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int r2 = dx * dx + dz * dz;
					if (r2 > outer * outer + outer || r2 < (outer - 1) * (outer - 1)) {
						continue;
					}
					if (dz < 0 && Math.abs(dx) <= 1 && h <= 3) {
						continue;
					}
					local(level, box, dx, h, dz, (h + dx + dz) % 3 == 0 ? scorched : bricks);
				}
			}
		}
		local(level, box, 0, radius + 1, 0, b(ModBlocks.EMBER_ROCK));
		local(level, box, 3, 1, 3, b(Blocks.SOUL_LANTERN));
		local(level, box, -3, 1, 3, b(Blocks.SOUL_LANTERN));
	}

	private void buildRuin(WorldGenLevel level, BoundingBox box) {
		groundPlate(level, box, 7);
		BlockState bricks = b(ModBlocks.VOIDSTONE_BRICKS);
		BlockState scorched = b(ModBlocks.SCORCHED_END_STONE);
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != 6 || (dz == -6 && Math.abs(dx) <= 1)) {
					continue;
				}
				int height = 1 + Math.round((BasinSites.noise(dx * 2 + 100, dz * 2 + 100, 3, 93 + (int) (seed & 7)) + 1.0F) * 2.6F);
				for (int y = 1; y <= height; y++) {
					local(level, box, dx, y, dz, y % 3 == 0 ? scorched : bricks);
				}
			}
		}
		for (int[] c : new int[][] {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
			int height = 2 + Math.round(BasinSites.hash(c[0], c[1], (int) (seed & 255)) * 4.0F);
			for (int y = 1; y <= height; y++) {
				local(level, box, c[0], y, c[1], b(ModBlocks.POLISHED_VOIDSTONE));
			}
		}
	}

	private void buildObeliskCourt(WorldGenLevel level, BoundingBox box) {
		groundPlate(level, box, 7);
		BlockState wall = b(ModBlocks.VOIDSTONE_BRICK_WALL);
		BlockState polished = b(ModBlocks.POLISHED_VOIDSTONE);
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) == 7 && !(dz == -7 && Math.abs(dx) <= 1)) {
					local(level, box, dx, 1, dz, wall);
				}
			}
		}
		for (int[] c : new int[][] {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
			for (int y = 1; y <= 6; y++) {
				local(level, box, c[0], y, c[1], polished);
			}
			local(level, box, c[0], 7, c[1], b(ModBlocks.EMBER_ROCK));
		}
		local(level, box, 0, 1, 5, b(Blocks.SOUL_LANTERN));
	}

	private void openTop(WorldGenLevel level, BoundingBox box) {
		int[] top = RING[(gy - floorY - 1) % RING.length];
		for (int[] cell : RING) {
			if (cell[0] != top[0] || cell[1] != top[1]) {
				put(level, box, cx + cell[0], gy, cz + cell[1], b(Blocks.AIR));
			}
		}
		put(level, box, cx, gy, cz, b(ModBlocks.POLISHED_VOIDSTONE));
		put(level, box, cx, gy + 1, cz, b(Blocks.SOUL_LANTERN));
	}
}
