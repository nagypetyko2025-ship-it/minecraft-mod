package hu.endrealms.structure;

import hu.endrealms.entity.RiftLord;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

/**
 * Rift Sanctum: a small roofed shrine on the surface, a spiral stair shaft under it, and north of the arena's wall the
 * opening into a domed round arena with six pillars. The Rift Lord waits at the arena's centre. The arena has its own
 * shell, so it stays closed even where the island above or around it is thin.
 */
public class RiftSanctumPiece extends StructurePiece {
	/** The shaft and shrine stand this far north of the arena centre. */
	public static final int SHAFT_OFFSET = 20;
	private static final int RADIUS = 16;
	private static final int HEIGHT = 12;
	private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

	private final int cx;
	private final int shrineY;
	private final int floorY;
	private final int cz;

	public RiftSanctumPiece(int cx, int shrineY, int floorY, int cz) {
		super(ModStructures.RIFT_SANCTUM_PIECE, 0, new BoundingBox(cx - RADIUS - 3, floorY - 3, cz - SHAFT_OFFSET - 7, cx + RADIUS + 3, shrineY + 10, cz + RADIUS + 3));
		this.cx = cx;
		this.shrineY = shrineY;
		this.floorY = floorY;
		this.cz = cz;
	}

	public RiftSanctumPiece(CompoundTag tag) {
		super(ModStructures.RIFT_SANCTUM_PIECE, tag);
		this.cx = tag.getIntOr("CX", 0);
		this.shrineY = tag.getIntOr("ShrineY", 64);
		this.floorY = tag.getIntOr("FloorY", 40);
		this.cz = tag.getIntOr("CZ", 0);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", cx);
		tag.putInt("ShrineY", shrineY);
		tag.putInt("FloorY", floorY);
		tag.putInt("CZ", cz);
	}

	private static void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);
		if (box.isInside(pos)) {
			level.setBlock(pos, state, 2);
		}
	}

	private int ceiling(double distance) {
		return floorY + HEIGHT - (int) (4.0 * distance * distance / (RADIUS * RADIUS));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unused, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		buildArena(level, box);
		buildShaft(level, box);
		buildShrine(level, box);
		BlockPos centre = new BlockPos(cx, floorY + 1, cz);
		if (box.isInside(centre)) {
			RiftLord boss = ModEntities.RIFT_LORD.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (boss != null) {
				boss.snapTo(cx + 0.5, floorY + 1, cz + 0.5, 180.0F, 0.0F);
				boss.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), EntitySpawnReason.STRUCTURE, null);
				boss.guardArena(centre);
				level.addFreshEntityWithPassengers(boss);
			}
		}
	}

	private BlockState floorTile(int dx, int dz, double distance) {
		if (distance < 2.0) {
			return ModBlocks.PRISM_LAMP.defaultBlockState();
		}
		if ((distance >= 5.0 && distance < 6.0) || (distance >= 11.0 && distance < 12.0)) {
			return ModBlocks.CRYSTALLINE_VOIDSTONE.defaultBlockState();
		}
		return dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz) ? ModBlocks.POLISHED_VOIDSTONE.defaultBlockState() : ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
	}

	private void buildArena(WorldGenLevel level, BoundingBox box) {
		BlockState bricks = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int dx = -RADIUS - 2; dx <= RADIUS + 2; dx++) {
			for (int dz = -RADIUS - 2; dz <= RADIUS + 2; dz++) {
				double distance = Math.sqrt(dx * dx + dz * dz);
				if (distance > RADIUS + 1.5) {
					continue;
				}
				boolean wall = distance > RADIUS;
				int top = ceiling(distance);
				for (int y = floorY - 2; y <= top + 1; y++) {
					BlockState state;
					if (wall || y >= top || y < floorY) {
						state = bricks;
					} else if (y == floorY) {
						state = floorTile(dx, dz, distance);
					} else {
						state = air;
					}
					put(level, box, cx + dx, y, cz + dz, state);
				}
			}
		}
		BlockState lamp = ModBlocks.PRISM_LAMP.defaultBlockState();
		for (int i = 0; i < 8; i++) {
			float angle = i * Mth.TWO_PI / 8.0F;
			int dx = Math.round(Mth.cos(angle) * 9.0F);
			int dz = Math.round(Mth.sin(angle) * 9.0F);
			put(level, box, cx + dx, ceiling(Math.sqrt(dx * dx + dz * dz)), cz + dz, lamp);
		}
		BlockState polished = ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
		for (int i = 0; i < 6; i++) {
			float angle = i * Mth.TWO_PI / 6.0F;
			int dx = Math.round(Mth.cos(angle) * 11.0F);
			int dz = Math.round(Mth.sin(angle) * 11.0F);
			int top = ceiling(11.0);
			for (int y = floorY + 1; y < top; y++) {
				put(level, box, cx + dx, y, cz + dz, y == floorY + 5 ? lamp : polished);
				for (Direction side : Direction.Plane.HORIZONTAL) {
					put(level, box, cx + dx + side.getStepX(), y, cz + dz + side.getStepZ(), bricks);
				}
			}
		}
	}

	private void buildShaft(WorldGenLevel level, BoundingBox box) {
		int sz = cz - SHAFT_OFFSET;
		BlockState bricks = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		BlockState polished = ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int y = floorY; y < shrineY; y++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean shell = y == floorY || Math.abs(dx) == 2 || Math.abs(dz) == 2;
					put(level, box, cx + dx, y, sz + dz, shell ? bricks : air);
				}
			}
			put(level, box, cx, y, sz, polished);
		}
		int steps = shrineY - floorY;
		for (int i = 0; i < steps; i++) {
			int[] cell = RING[i % RING.length];
			int[] next = RING[(i + 1) % RING.length];
			Direction facing = Direction.getApproximateNearest(next[0] - cell[0], 0, next[1] - cell[1]);
			put(level, box, cx + cell[0], floorY + 1 + i, sz + cell[1], ModBlocks.VOIDSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
		}
		// doorway from the bottom of the shaft through the arena wall
		for (int dx = -1; dx <= 1; dx++) {
			for (int z = sz + 2; z <= sz + 5; z++) {
				for (int y = floorY + 1; y <= floorY + 3; y++) {
					put(level, box, cx + dx, y, z, air);
				}
			}
		}
	}

	private void buildShrine(WorldGenLevel level, BoundingBox box) {
		int sz = cz - SHAFT_OFFSET;
		BlockState bricks = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		BlockState polished = ModBlocks.POLISHED_VOIDSTONE.defaultBlockState();
		BlockState crystal = ModBlocks.CRYSTALLINE_VOIDSTONE.defaultBlockState();
		BlockState lamp = ModBlocks.PRISM_LAMP.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				for (int y = shrineY - 1; y >= shrineY - 8; y--) {       // foundation down to the ground
					BlockPos pos = new BlockPos(cx + dx, y, sz + dz);
					if (ring > 2 && box.isInside(pos) && level.getBlockState(pos).canBeReplaced()) {
						level.setBlock(pos, bricks, 2);
					}
				}
				if (ring <= 1 && !(dx == 0 && dz == 0)) {
					int[] topStep = RING[(shrineY - floorY - 1) % RING.length];
					if (dx != topStep[0] || dz != topStep[1]) {
						put(level, box, cx + dx, shrineY, sz + dz, air);
					}
				} else {
					put(level, box, cx + dx, shrineY, sz + dz, ring == 5 && Math.abs(dx) == Math.abs(dz) ? crystal : polished);
				}
				for (int y = 1; y <= 9; y++) {
					put(level, box, cx + dx, shrineY + y, sz + dz, air);
				}
			}
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int szs = -1; szs <= 1; szs += 2) {
				for (int y = 1; y <= 5; y++) {
					put(level, box, cx + sx * 4, shrineY + y, sz + szs * 4, y == 3 ? lamp : bricks);
				}
			}
		}
		put(level, box, cx, shrineY, sz, polished);
		put(level, box, cx, shrineY + 1, sz, polished);
		put(level, box, cx, shrineY + 2, sz, lamp);
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				if (ring >= 3) {
					put(level, box, cx + dx, shrineY + 6, sz + dz, ring == 5 ? polished : bricks);
				}
				if (ring >= 2 && ring <= 3) {
					put(level, box, cx + dx, shrineY + 7, sz + dz, bricks);
				}
				if (ring <= 2) {
					put(level, box, cx + dx, shrineY + 8, sz + dz, ring == 0 ? lamp : polished);
				}
			}
		}
	}
}
