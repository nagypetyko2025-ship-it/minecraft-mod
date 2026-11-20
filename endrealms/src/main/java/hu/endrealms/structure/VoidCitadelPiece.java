package hu.endrealms.structure;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.AshWraith;
import hu.endrealms.entity.VoidKing;
import hu.endrealms.entity.VoidSentinel;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModEntities;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Void Citadel: a walled castle alone on a big island, six small islands chained to it. The Void King holds the throne
 * room of the keep, and a sealed treasury lies under the hall. The piece builds its own terrain and first empties
 * everything within {@link #REACH}, so nothing else stands on the island. Coordinates in the build methods are relative
 * to the island centre, north is negative z, and the gate faces south.
 */
public class VoidCitadelPiece extends StructurePiece {
	/** A structure may reach at most eight chunks from its start chunk. */
	public static final int REACH = 134;

	private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/void_citadel"));
	private static final ResourceKey<LootTable> TREASURE = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/void_citadel_treasure"));
	private static final int ISLAND_RADIUS = 68;
	private static final int PLATEAU = 56;
	private static final int ISLES = 6;
	private static final int ISLE_DISTANCE = 104;
	private static final int HALF = 34;
	private static final int TOWER_RADIUS = 6;

	private final int cx;
	private final int floor;
	private final int cz;
	private final int seed;

	private record Isle(int index, int x, int z, int y, float radius, float angle) {
	}

	public VoidCitadelPiece(int cx, int floor, int cz, int seed) {
		super(ModStructures.VOID_CITADEL_PIECE, 0, new BoundingBox(cx - REACH, 0, cz - REACH, cx + REACH, floor + 60, cz + REACH));
		this.cx = cx;
		this.floor = floor;
		this.cz = cz;
		this.seed = seed;
	}

	public VoidCitadelPiece(CompoundTag tag) {
		super(ModStructures.VOID_CITADEL_PIECE, tag);
		this.cx = tag.getIntOr("CX", 0);
		this.floor = tag.getIntOr("Floor", 70);
		this.cz = tag.getIntOr("CZ", 0);
		this.seed = tag.getIntOr("Seed", 0);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", cx);
		tag.putInt("Floor", floor);
		tag.putInt("CZ", cz);
		tag.putInt("Seed", seed);
	}

	/** Writes into the one chunk being generated; everything outside that chunk is built when its own turn comes. */
	private final class Build {
		final WorldGenLevel level;
		final BoundingBox box;
		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		Build(WorldGenLevel level, BoundingBox box) {
			this.level = level;
			this.box = box;
		}

		boolean inside(int x, int y, int z) {
			return box.isInside(pos.set(cx + x, y, cz + z));
		}

		void set(int x, int y, int z, BlockState state) {
			if (inside(x, y, z)) {
				level.setBlock(pos, state, 2);
			}
		}

		void setIfFree(int x, int y, int z, BlockState state) {
			if (inside(x, y, z) && level.getBlockState(pos).canBeReplaced()) {
				level.setBlock(pos, state, 2);
			}
		}

		void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
			int fromX = Math.max(cx + Math.min(x0, x1), box.minX());
			int toX = Math.min(cx + Math.max(x0, x1), box.maxX());
			int fromZ = Math.max(cz + Math.min(z0, z1), box.minZ());
			int toZ = Math.min(cz + Math.max(z0, z1), box.maxZ());
			for (int x = fromX; x <= toX; x++) {
				for (int z = fromZ; z <= toZ; z++) {
					for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
						level.setBlock(pos.set(x, y, z), state, 2);
					}
				}
			}
		}

		void chest(int x, int y, int z, Direction facing, ResourceKey<LootTable> table) {
			if (inside(x, y, z)) {
				BlockPos at = pos.immutable();
				level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
				RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(seed ^ at.asLong()), at, table);
			}
		}

		<T extends Mob> void mob(EntityType<T> type, int x, int y, int z) {
			if (!inside(x, y, z)) {
				return;
			}
			BlockPos at = pos.immutable();
			T mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (mob != null) {
				mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
				mob.setPersistenceRequired();
				level.addFreshEntityWithPassengers(mob);
			}
		}

		void sentinel(int x, int y, int z) {
			EntityType<VoidSentinel> type = ModEntities.VOID_SENTINEL;
			mob(type, x, y, z);
		}

		void wraith(int x, int y, int z) {
			EntityType<AshWraith> type = ModEntities.ASH_WRAITH;
			mob(type, x, y, z);
		}
	}

	private static BlockState bricks() {
		return ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
	}

	private static BlockState polished() {
		return ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
	}

	private static BlockState crystal() {
		return ModBlocks.CRYSTALLINE_VOIDSTONE.defaultBlockState();
	}

	private static BlockState lamp() {
		return ModBlocks.PRISM_LAMP.defaultBlockState();
	}

	private static BlockState air() {
		return Blocks.AIR.defaultBlockState();
	}

	private static BlockState glass() {
		return Blocks.STAINED_GLASS.purple().defaultBlockState();
	}

	private Isle isle(int index) {
		float angle = index * Mth.TWO_PI / ISLES + (BasinSites.hash(seed, index, 71) - 0.5F) * 0.35F;
		int x = Math.round(Mth.cos(angle) * ISLE_DISTANCE);
		int z = Math.round(Mth.sin(angle) * ISLE_DISTANCE);
		int y = floor - 8 + (int) (BasinSites.hash(seed, index, 72) * 16.0F);
		return new Isle(index, x, z, y, 12.0F + 4.0F * BasinSites.hash(seed, index, 73), angle);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unused, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		Build b = new Build(level, box);
		terrain(b);
		courtyard(b);
		curtainWall(b);
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				tower(b, sx, sz);
			}
		}
		gatehouse(b);
		sideHall(b, -1);
		sideHall(b, 1);
		keep(b);
		throneRoom(b);
		treasury(b);
		for (int i = 0; i < ISLES; i++) {
			Isle isle = isle(i);
			smallIsland(b, isle);
			chain(b, isle);
		}
		garrison(b);
	}

	// ---------------------------------------------------------------- terrain

	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	private void terrain(Build b) {
		BlockState stone = ModBlocks.VOIDSTONE.defaultBlockState();
		BlockState end = Blocks.END_STONE.defaultBlockState();
		Isle[] isles = new Isle[ISLES];
		for (int i = 0; i < ISLES; i++) {
			isles[i] = isle(i);
		}
		for (int x = b.box.minX(); x <= b.box.maxX(); x++) {
			for (int z = b.box.minZ(); z <= b.box.maxZ(); z++) {
				int dx = x - cx;
				int dz = z - cz;
				if (dx * dx + dz * dz > REACH * REACH) {
					continue;
				}
				int[] span = column(x, z, dx, dz, isles);
				int top = b.level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				for (int y = b.level.getMinY(); y <= top; y++) {
					if (span == null || y < span[0] || y > span[1]) {
						b.level.setBlock(b.pos.set(x, y, z), air(), 2);
					}
				}
				if (span != null) {
					for (int y = span[0]; y <= span[1]; y++) {
						b.level.setBlock(b.pos.set(x, y, z), span[1] - y < 2 || y - span[0] < 4 ? stone : end, 2);
					}
				}
			}
		}
	}

	/** {bottom, surface} of the land in a column, or null over the void. */
	private int[] column(int x, int z, int dx, int dz, Isle[] isles) {
		float wobble = BasinSites.noise(x, z, 16, 61);
		float r = Mth.sqrt(dx * dx + dz * dz);
		float radius = ISLAND_RADIUS + 6.0F * wobble;
		if (r <= radius) {
			int surface = r <= PLATEAU ? floor
					: floor - Math.round(7.0F * smooth((r - PLATEAU) / (radius - PLATEAU)) - 1.5F * BasinSites.noise(x, z, 9, 62));
			float depth = Mth.sqrt(Math.max(0.0F, 1.0F - Mth.square(r / radius)));
			return new int[] {floor - 6 - Math.round(36.0F * depth * (0.8F + 0.2F * BasinSites.noise(x, z, 12, 63))), surface};
		}
		for (Isle isle : isles) {
			float d = Mth.sqrt(Mth.square(dx - isle.x()) + Mth.square(dz - isle.z()));
			float limit = isle.radius() + 2.5F * BasinSites.noise(x, z, 8, 64 + isle.index());
			if (d <= limit) {
				float depth = Mth.sqrt(Math.max(0.0F, 1.0F - Mth.square(d / limit)));
				return new int[] {isle.y() - 2 - Math.round(16.0F * depth), isle.y() - Math.round(3.0F * smooth((d - limit * 0.55F) / (limit * 0.45F)))};
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- the castle grounds

	private void courtyard(Build b) {
		for (int x = -HALF; x <= HALF; x++) {
			for (int z = -HALF; z <= HALF; z++) {
				b.set(x, floor, z, (x + z & 1) == 0 ? polished() : bricks());
			}
		}
		b.fill(-3, floor, 35, 3, floor, PLATEAU - 4, polished());
		for (int z = 12; z <= 28; z += 8) {
			for (int side = -1; side <= 1; side += 2) {
				b.fill(side * 12, floor + 1, z, side * 12, floor + 3, z, polished());
				b.set(side * 12, floor + 4, z, lamp());
			}
		}
	}

	private void curtainWall(Build b) {
		for (int side = -1; side <= 1; side += 2) {
			b.fill(side * (HALF - 1), floor - 3, -HALF, side * HALF, floor + 9, HALF, bricks());
			b.fill(-HALF, floor - 3, side * (HALF - 1), HALF, floor + 9, side * HALF, bricks());
		}
		for (int i = -HALF; i <= HALF; i++) {
			if ((i & 1) == 0) {
				for (int side = -1; side <= 1; side += 2) {
					b.set(side * HALF, floor + 10, i, bricks());
					b.set(i, floor + 10, side * HALF, bricks());
				}
			}
			if (i % 8 == 0) {
				for (int side = -1; side <= 1; side += 2) {
					b.set(side * (HALF - 1), floor + 9, i, lamp());
					b.set(i, floor + 9, side * (HALF - 1), lamp());
				}
			}
		}
	}

	private void tower(Build b, int sx, int sz) {
		int tx = sx * HALF;
		int tz = sz * HALF;
		for (int dx = -TOWER_RADIUS; dx <= TOWER_RADIUS; dx++) {
			for (int dz = -TOWER_RADIUS; dz <= TOWER_RADIUS; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > TOWER_RADIUS + 0.5) {
					continue;
				}
				boolean ring = d > TOWER_RADIUS - 1.4;
				b.fill(tx + dx, floor - 3, tz + dz, tx + dx, floor - 1, tz + dz, bricks());
				b.set(tx + dx, floor, tz + dz, polished());
				for (int y = floor + 1; y <= floor + 15; y++) {
					b.set(tx + dx, y, tz + dz, ring ? bricks() : air());
				}
				b.set(tx + dx, floor + 16, tz + dz, ring ? bricks() : polished());
				if (ring && (dx + dz & 1) == 0) {
					b.set(tx + dx, floor + 17, tz + dz, bricks());
				}
			}
		}
		b.set(tx, floor + 17, tz, lamp());
		// a ladder on the wall facing the courtyard, up to the roof
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, sz > 0 ? Direction.SOUTH : Direction.NORTH);
		for (int y = floor + 1; y <= floor + 16; y++) {
			b.set(tx, y, tz - sz * 4, ladder);
		}
		// door from the courtyard
		for (int z = sz * (HALF - 4); sz > 0 ? z <= sz * (HALF - 3) : z >= sz * (HALF - 3); z += sz) {
			for (int x = sx * (HALF - 7); sx > 0 ? x <= sx * (HALF - 1) : x >= sx * (HALF - 1); x += sx) {
				b.fill(x, floor + 1, z, x, floor + 3, z, air());
			}
		}
		b.chest(tx + sx, floor + 1, tz + sz, sz > 0 ? Direction.NORTH : Direction.SOUTH, LOOT);
	}

	private void gatehouse(Build b) {
		for (int side = -1; side <= 1; side += 2) {
			b.fill(side * 3, floor - 3, HALF - 3, side * 7, floor + 13, HALF + 3, bricks());
			for (int x = 3; x <= 7; x++) {
				for (int z = HALF - 3; z <= HALF + 3; z++) {
					if ((x + z & 1) == 0 && (x == 3 || x == 7 || z == HALF - 3 || z == HALF + 3)) {
						b.set(side * x, floor + 14, z, bricks());
					}
				}
			}
			b.set(side * 5, floor + 14, HALF, lamp());
		}
		b.fill(-2, floor + 7, HALF - 3, 2, floor + 13, HALF + 3, bricks());
		b.fill(-2, floor + 1, HALF - 3, 2, floor + 6, HALF + 3, air());
		b.fill(-2, floor, HALF - 3, 2, floor, HALF + 3, polished());
		b.fill(-2, floor + 5, HALF + 3, 2, floor + 6, HALF + 3, Blocks.IRON_BARS.defaultBlockState());
	}

	private void sideHall(Build b, int side) {
		int inner = side * 22;
		int outer = side * 31;
		b.fill(inner, floor, -28, outer, floor + 8, 2, bricks());
		b.fill(side * 23, floor + 1, -27, side * 30, floor + 7, 1, air());
		b.fill(side * 23, floor, -27, side * 30, floor, 1, polished());
		b.fill(side * 25, floor + 1, 2, side * 27, floor + 3, 2, air());
		for (int z = -24; z <= -2; z += 6) {
			b.fill(inner, floor + 3, z, inner, floor + 5, z + 1, glass());
			b.set(side * 26, floor + 7, z, lamp());
		}
		b.fill(inner, floor + 9, -28, outer, floor + 9, 2, polished());
		for (int z = -26; z <= -18; z += 4) {
			b.chest(side * 30, floor + 1, z, side > 0 ? Direction.WEST : Direction.EAST, LOOT);
		}
	}

	private void keep(Build b) {
		b.fill(-19, floor - 3, -30, 19, floor + 15, 6, bricks());
		b.fill(-19, floor + 16, -30, 19, floor + 16, 6, polished());
		for (int x = -19; x <= 19; x++) {
			if ((x & 1) == 0) {
				b.set(x, floor + 17, -30, bricks());
				b.set(x, floor + 17, 6, bricks());
			}
		}
		for (int z = -30; z <= 6; z++) {
			if ((z & 1) == 0) {
				b.set(-19, floor + 17, z, bricks());
				b.set(19, floor + 17, z, bricks());
			}
		}
		// the great tower on the roof
		b.fill(-7, floor + 17, -21, 7, floor + 29, -7, bricks());
		b.fill(-6, floor + 17, -20, 6, floor + 28, -8, air());
		b.fill(-7, floor + 29, -21, 7, floor + 29, -7, polished());
		for (int i = -7; i <= 7; i++) {
			if ((i & 1) != 0) {
				b.set(i, floor + 30, -21, bricks());
				b.set(i, floor + 30, -7, bricks());
				b.set(-7, floor + 30, -14 + i, bricks());
				b.set(7, floor + 30, -14 + i, bricks());
			}
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				b.fill(sx * 7, floor + 30, -14 + sz * 7, sx * 7, floor + 35, -14 + sz * 7, crystal());
				b.set(sx * 7, floor + 36, -14 + sz * 7, lamp());
			}
		}
		// the front: buttresses between tall windows
		for (int side = -1; side <= 1; side += 2) {
			for (int x = 5; x <= 17; x += 6) {
				b.fill(side * x, floor + 1, 7, side * x, floor + 15, 8, polished());
				b.set(side * x, floor + 16, 7, lamp());
			}
			for (int x = 8; x <= 14; x += 6) {
				b.fill(side * x, floor + 9, 6, side * (x + 1), floor + 13, 6, glass());
			}
		}
		// entrance hall, then the doorway into the throne room
		b.fill(-15, floor + 1, 0, 15, floor + 7, 4, air());
		b.fill(-15, floor, 0, 15, floor, 4, polished());
		b.fill(-2, floor + 1, 5, 2, floor + 5, 6, air());
		b.fill(-2, floor + 1, -2, 2, floor + 5, -1, air());
		for (int x = -12; x <= 12; x += 8) {
			b.set(x, floor + 7, 2, lamp());
		}
	}

	private void throneRoom(Build b) {
		b.fill(-15, floor + 1, -28, 15, floor + 14, -3, air());
		for (int x = -15; x <= 15; x++) {
			for (int z = -28; z <= -3; z++) {
				boolean aisle = Math.abs(x) <= 2;
				b.set(x, floor, z, aisle || ((x ^ z) & 1) != 0 ? polished() : bricks());
			}
		}
		for (int z = -24; z <= -3; z++) {
			b.fill(-1, floor + 1, z, 1, floor + 1, z, Blocks.CARPET.purple().defaultBlockState());
		}
		for (int side = -1; side <= 1; side += 2) {
			for (int z = -24; z <= -10; z += 7) {
				b.fill(side * 11, floor + 1, z, side * 12, floor + 14, z + 1, polished());
				b.fill(side * 11, floor + 7, z, side * 12, floor + 7, z + 1, lamp());
			}
			for (int z = -25; z <= -6; z += 6) {
				b.fill(side * 16, floor + 6, z, side * 19, floor + 11, z + 1, glass());
			}
		}
		for (int x = -7; x <= 7; x += 7) {
			for (int z = -22; z <= -6; z += 8) {
				b.set(x, floor + 15, z, lamp());
			}
		}
		// dais and throne against the north wall
		b.fill(-6, floor + 1, -28, 6, floor + 1, -26, polished());
		b.fill(-3, floor + 2, -28, 3, floor + 2, -27, polished());
		BlockState seat = ModBlocks.VOIDSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
		b.set(0, floor + 3, -27, seat);
		b.set(-1, floor + 3, -27, polished());
		b.set(1, floor + 3, -27, polished());
		b.fill(-1, floor + 3, -28, 1, floor + 8, -28, crystal());
		b.set(0, floor + 9, -28, lamp());
		// the seal over the treasury shaft, opened by the King's death
		b.fill(-1, floor, -23, 1, floor, -21, crystal());
		BlockPos centre = new BlockPos(cx, floor + 1, cz - 15);
		if (b.box.isInside(centre)) {
			VoidKing king = ModEntities.VOID_KING.create(b.level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (king != null) {
				king.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0.0F, 0.0F);
				king.finalizeSpawn(b.level, b.level.getCurrentDifficultyAt(centre), EntitySpawnReason.STRUCTURE, null);
				king.guardArena(centre);
				b.level.addFreshEntityWithPassengers(king);
			}
		}
	}

	private void treasury(Build b) {
		int base = floor - 8;
		b.fill(-10, base - 1, -28, 10, floor - 2, -12, bricks());
		b.fill(-9, base + 1, -27, 9, floor - 3, -13, air());
		b.fill(-9, base, -27, 9, base, -13, polished());
		b.fill(-1, floor - 2, -23, 1, floor - 1, -21, air());
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
		b.fill(-1, base + 1, -24, 1, floor - 3, -24, bricks());
		b.fill(-1, base + 1, -23, 1, floor - 1, -23, ladder);
		for (int z = -25; z <= -16; z += 3) {
			b.chest(-9, base + 1, z, Direction.EAST, TREASURE);
			b.chest(9, base + 1, z, Direction.WEST, TREASURE);
		}
		b.chest(-3, base + 1, -13, Direction.NORTH, TREASURE);
		b.chest(3, base + 1, -13, Direction.NORTH, TREASURE);
		for (int x = -6; x <= 6; x += 6) {
			b.set(x, floor - 2, -18, lamp());
		}
		for (int x = -5; x <= 5; x += 10) {
			b.set(x, base, -16, crystal());
			b.set(x, base, -24, crystal());
		}
	}

	// ---------------------------------------------------------------- the small islands

	private void smallIsland(Build b, Isle isle) {
		int x = isle.x();
		int z = isle.z();
		int y = isle.y();
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				if (dx * dx + dz * dz <= 34) {
					b.fill(x + dx, y - 2, z + dz, x + dx, y - 1, z + dz, bricks());
					b.set(x + dx, y, z + dz, (dx + dz & 1) == 0 ? polished() : bricks());
				}
			}
		}
		switch (isle.index() % 3) {
			case 0 -> watchtower(b, x, y, z);
			case 1 -> shrine(b, x, y, z);
			default -> ruin(b, x, y, z);
		}
	}

	private void watchtower(Build b, int x, int y, int z) {
		b.fill(x - 3, y + 1, z - 3, x + 3, y + 11, z + 3, bricks());
		b.fill(x - 2, y + 1, z - 2, x + 2, y + 10, z + 2, air());
		b.fill(x - 3, y + 11, z - 3, x + 3, y + 11, z + 3, polished());
		for (int i = -3; i <= 3; i += 2) {
			b.set(x + i, y + 12, z - 3, bricks());
			b.set(x + i, y + 12, z + 3, bricks());
			b.set(x - 3, y + 12, z + i, bricks());
			b.set(x + 3, y + 12, z + i, bricks());
		}
		b.fill(x - 1, y + 1, z + 3, x + 1, y + 3, z + 3, air());
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
		b.fill(x, y + 1, z - 2, x, y + 11, z - 2, ladder);
		b.set(x + 2, y + 10, z + 2, lamp());
		b.chest(x + 2, y + 1, z - 2, Direction.WEST, LOOT);
		b.chest(x - 2, y + 1, z - 2, Direction.EAST, LOOT);
	}

	private void shrine(Build b, int x, int y, int z) {
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				b.fill(x + sx * 4, y + 1, z + sz * 4, x + sx * 4, y + 6, z + sz * 4, polished());
			}
		}
		b.fill(x - 5, y + 7, z - 5, x + 5, y + 7, z + 5, ModBlocks.VOIDSTONE_BRICK_SLAB.defaultBlockState());
		b.fill(x - 2, y + 8, z - 2, x + 2, y + 8, z + 2, polished());
		b.set(x, y + 9, z, lamp());
		b.fill(x - 1, y + 1, z - 1, x + 1, y + 1, z + 1, crystal());
		b.chest(x, y + 2, z, Direction.SOUTH, LOOT);
		b.chest(x - 3, y + 1, z, Direction.EAST, LOOT);
	}

	private void ruin(Build b, int x, int y, int z) {
		b.fill(x - 5, y + 1, z - 4, x - 4, y + 7, z - 3, bricks());
		b.fill(x - 3, y + 1, z - 4, x + 2, y + 4, z - 4, bricks());
		b.fill(x + 3, y + 1, z - 4, x + 4, y + 2, z - 4, bricks());
		b.fill(x + 4, y + 1, z + 1, x + 5, y + 5, z + 2, bricks());
		b.set(x - 4, y + 8, z - 3, lamp());
		b.chest(x, y + 1, z - 3, Direction.SOUTH, LOOT);
		b.chest(x + 3, y + 1, z + 1, Direction.WEST, LOOT);
	}

	/** A chain from the rim of the big island to a small one, sagging in the middle, with stone links to stand on. */
	private void chain(Build b, Isle isle) {
		float cos = Mth.cos(isle.angle());
		float sin = Mth.sin(isle.angle());
		int x = Math.round(cos * (ISLAND_RADIUS - 8));
		int z = Math.round(sin * (ISLAND_RADIUS - 8));
		int endX = isle.x() - Math.round(cos * (isle.radius() - 3.0F));
		int endZ = isle.z() - Math.round(sin * (isle.radius() - 3.0F));
		int startY = floor - 3;
		int endY = isle.y() - 2;
		int spanX = Math.abs(endX - x);
		int spanZ = Math.abs(endZ - z);
		int total = spanX + spanZ;
		int stepX = Integer.signum(endX - x);
		int stepZ = Integer.signum(endZ - z);
		int previousY = startY;
		for (int step = 0; step <= total; step++) {
			float t = step / (float) Math.max(1, total);
			int y = Math.round(Mth.lerp(t, startY, endY) - 4.0F * Mth.sin(Mth.PI * t));
			boolean alongX = spanZ == 0 || (spanX != 0 && Math.abs(endX - x) * spanZ >= Math.abs(endZ - z) * spanX);
			if (y != previousY || step % 9 == 0) {
				b.setIfFree(x, y, z, polished());
				b.setIfFree(x, previousY, z, polished());
			} else {
				b.setIfFree(x, y, z, Blocks.IRON_CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z));
			}
			previousY = y;
			if (alongX) {
				x += stepX;
			} else {
				z += stepZ;
			}
		}
	}

	// ---------------------------------------------------------------- enemies

	private void garrison(Build b) {
		int ground = floor + 1;
		int[][] yard = {{-24, 10}, {24, 10}, {-24, 22}, {24, 22}, {-14, 26}, {14, 26}, {-6, 14}, {6, 14}, {-6, 28}, {6, 28}, {-26, 28}, {26, 28}};
		for (int[] p : yard) {
			b.sentinel(p[0], ground, p[1]);
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				b.sentinel(sx * HALF - sx * 2, ground, sz * HALF);
				b.sentinel(sx * HALF, ground, sz * HALF - sz * 2);
			}
		}
		int[][] walls = {{-20, -33}, {20, -33}, {-33, -14}, {33, -14}, {-33, 14}, {33, 14}, {-16, 33}, {16, 33}};
		for (int[] p : walls) {
			b.wraith(p[0], floor + 10, p[1]);
		}
		for (int x = -12; x <= 12; x += 6) {
			b.sentinel(x, ground, 2);
		}
		for (int side = -1; side <= 1; side += 2) {
			for (int z = -22; z <= -2; z += 5) {
				b.sentinel(side * 26, ground, z);
			}
		}
		for (int i = 0; i < ISLES; i++) {
			Isle isle = isle(i);
			b.sentinel(isle.x() - 3, isle.y() + 1, isle.z() + 4);
			b.wraith(isle.x() + 3, isle.y() + 1, isle.z() + 4);
		}
	}
}
