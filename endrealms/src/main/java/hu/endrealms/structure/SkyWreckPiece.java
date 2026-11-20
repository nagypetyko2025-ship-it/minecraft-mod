package hu.endrealms.structure;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.EndVillager;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Sky Wreck: a trader's ship adrift in the void, held up by crystals on chains. Variants: a small sloop, a long
 * freighter, a ship snapped in two with a rotten gangway across, and a capsizing hulk, each in one of three liveries.
 * Part of the deck is rotten and drops under a player. Built in ship coordinates (u along the keel, w up, v across).
 */
public class SkyWreckPiece extends StructurePiece {
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/sky_wreck"));
	private static final int HALF = 28;
	private static final int SLOOP = 0;
	private static final int FREIGHTER = 1;
	private static final int BROKEN = 2;
	private static final int CAPSIZED = 3;

	private final int cx;
	private final int cy;
	private final int cz;
	private final boolean alongX;
	private final int flip;
	private final int type;
	private final int livery;
	private final long seed;

	// derived from the type and the seed
	private int halfLength;
	private int beam;
	private int depth;
	private float pitch;
	private float roll;
	private int gapAt;
	private int gapLength;
	private int gapDrop;

	public SkyWreckPiece(RandomSource random, int x, int y, int z) {
		this(x, y, z, random.nextBoolean(), random.nextBoolean() ? 1 : -1, random.nextInt(4), random.nextInt(3), random.nextLong());
	}

	private SkyWreckPiece(int x, int y, int z, boolean alongX, int flip, int type, int livery, long seed) {
		super(ModStructures.SKY_WRECK_PIECE, 0, new BoundingBox(x - HALF, y - 18, z - HALF, x + HALF, y + 26, z + HALF));
		this.cx = x;
		this.cy = y;
		this.cz = z;
		this.alongX = alongX;
		this.flip = flip;
		this.type = type;
		this.livery = livery;
		this.seed = seed;
		derive();
	}

	public SkyWreckPiece(CompoundTag tag) {
		super(ModStructures.SKY_WRECK_PIECE, tag);
		this.cx = tag.getIntOr("CX", 0);
		this.cy = tag.getIntOr("CY", 80);
		this.cz = tag.getIntOr("CZ", 0);
		this.alongX = tag.getBooleanOr("AlongX", true);
		this.flip = tag.getIntOr("Flip", 1);
		this.type = tag.getIntOr("Type", 0);
		this.livery = tag.getIntOr("Livery", 0);
		this.seed = tag.getLongOr("Seed", 0L);
		derive();
	}

	private void derive() {
		RandomSource random = RandomSource.create(seed ^ 0x5EEDL);
		switch (type) {
			case FREIGHTER -> {
				halfLength = 12 + random.nextInt(3);
				beam = 4;
			}
			case BROKEN -> {
				halfLength = 10 + random.nextInt(2);
				beam = 3;
			}
			case CAPSIZED -> {
				halfLength = 9 + random.nextInt(2);
				beam = 3;
			}
			default -> {
				halfLength = 8 + random.nextInt(2);
				beam = 2 + random.nextInt(2);
			}
		}
		depth = beam + 1 + random.nextInt(2);
		pitch = (random.nextFloat() - 0.5F) * (type == CAPSIZED ? 0.3F : 0.12F);
		roll = type == CAPSIZED ? 0.55F * (random.nextBoolean() ? 1 : -1) : (random.nextFloat() - 0.5F) * 0.2F;
		gapAt = random.nextInt(3) - 1;
		gapLength = type == BROKEN ? 4 + random.nextInt(3) : 0;
		gapDrop = type == BROKEN ? random.nextInt(4) : 0;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", cx);
		tag.putInt("CY", cy);
		tag.putInt("CZ", cz);
		tag.putBoolean("AlongX", alongX);
		tag.putInt("Flip", flip);
		tag.putInt("Type", type);
		tag.putInt("Livery", livery);
		tag.putLong("Seed", seed);
	}

	// ---------------------------------------------------------------- ship coordinates

	/** World position of a ship cell, before the break of a snapped ship is applied. */
	private BlockPos rawPos(int u, int w, int v) {
		int y = cy + w + Math.round(u * pitch + v * roll);
		return alongX ? new BlockPos(cx + flip * u, y, cz + v) : new BlockPos(cx + v, y, cz + flip * u);
	}

	private BlockPos pos(int u, int w, int v) {
		return type == BROKEN && u > gapAt ? rawPos(u + gapLength, w - gapDrop, v) : rawPos(u, w, v);
	}

	private void put(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
		if (box.isInside(pos)) {
			level.setBlock(pos, state, 2);
		}
	}

	private int halfWidth(int u) {
		float t = Math.abs(u) / (halfLength + 0.5F);
		float profile = u > 0 ? 1.0F - (float) Math.pow(t, 2.4F) : 1.0F - (float) Math.pow(t, 6.0F);
		return Math.max(0, Math.round(beam * profile));
	}

	private int keelDepth(int u, int v) {
		float t = Math.abs(u) / (halfLength + 0.5F);
		int deepest = Math.max(1, Math.round(depth * (u > 0 ? 1.0F - t * t * t : 1.0F - (float) Math.pow(t, 5.0F))));
		float across = Math.abs(v) / (halfWidth(u) + 0.5F);
		return Math.round(deepest * Mth.sqrt(1.0F - across * across));
	}

	private BlockState hull() {
		return switch (livery) {
			case 1 -> ModBlocks.STRIPPED_ASTRAL_WOOD.defaultBlockState();
			case 2 -> ModBlocks.SMOOTH_VOID_SANDSTONE.defaultBlockState();
			default -> ModBlocks.ASTRAL_PLANKS.defaultBlockState();
		};
	}

	private BlockState sail() {
		return switch (livery) {
			case 1 -> Blocks.WOOL.cyan().defaultBlockState();
			case 2 -> Blocks.WOOL.magenta().defaultBlockState();
			default -> Blocks.WOOL.purple().defaultBlockState();
		};
	}

	// ---------------------------------------------------------------- building

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unused, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		RandomSource random = RandomSource.create(seed);
		int cabinEnd = -halfLength + 6;
		buildHull(level, box, random, cabinEnd);
		buildCabin(level, box, random, cabinEnd);
		buildHold(level, box, random, cabinEnd);
		if (type == FREIGHTER) {
			buildMast(level, box, random, -3, 8);
			buildMast(level, box, random, 6, 9);
		} else {
			buildMast(level, box, random, type == BROKEN ? -2 : 2, type == CAPSIZED ? 5 : 8);
		}
		if (type == BROKEN) {
			buildGangway(level, box);
		}
	}

	private void buildHull(WorldGenLevel level, BoundingBox box, RandomSource random, int cabinEnd) {
		BlockState hull = hull();
		BlockState stable = ModBlocks.ASTRAL_PLANKS.defaultBlockState();
		BlockState rotten = ModBlocks.CRUMBLING_PLANKS.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState rail = ModBlocks.ASTRAL_FENCE.defaultBlockState();
		for (int u = -halfLength; u <= halfLength; u++) {
			boolean fracture = type == BROKEN && Math.abs(u - gapAt) <= 1;
			int width = halfWidth(u);
			for (int v = -width; v <= width; v++) {
				boolean side = Math.abs(v) == width;
				int bottom = -keelDepth(u, v);
				for (int w = bottom; w <= 0; w++) {
					boolean torn = random.nextFloat() < (fracture ? 0.55F : type == CAPSIZED ? 0.09F : 0.03F);
					BlockState state;
					if (torn && u > cabinEnd) {
						state = air;
					} else if (w == bottom || side) {
						state = hull;
					} else if (w == 0) {
						boolean soft = u > cabinEnd && BasinSites.noise(u + 500, v + 500, 5, 61 + (int) (seed & 15)) > -0.15F;
						state = soft ? rotten : stable;
					} else {
						state = air;
					}
					put(level, box, pos(u, w, v), state);
				}
				if (side && random.nextFloat() > 0.2F) {
					put(level, box, pos(u, 1, v), rail);
				}
			}
		}
	}

	private void buildCabin(WorldGenLevel level, BoundingBox box, RandomSource random, int cabinEnd) {
		BlockState wall = ModBlocks.VOIDSTONE_BRICKS.defaultBlockState();
		BlockState roof = ModBlocks.POLISHED_VOIDSTONE_SLAB.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState glass = Blocks.STAINED_GLASS.purple().defaultBlockState();
		for (int u = -halfLength + 1; u <= cabinEnd; u++) {
			int width = Math.max(1, halfWidth(u) - 1);
			for (int v = -width; v <= width; v++) {
				boolean edge = u == -halfLength + 1 || u == cabinEnd || Math.abs(v) == width;
				for (int w = 1; w <= 3; w++) {
					BlockState state = !edge ? air : (w == 2 && v % 2 == 0 && Math.abs(v) == width) ? glass : wall;
					put(level, box, pos(u, w, v), state);
				}
				put(level, box, pos(u, 4, v), roof);
			}
		}
		for (int w = 1; w <= 2; w++) {
			put(level, box, pos(cabinEnd, w, 0), air);
		}
		put(level, box, pos(cabinEnd - 2, 3, 0), Blocks.END_ROD.defaultBlockState());
		chest(level, box, pos(-halfLength + 2, 1, 0));
		if (random.nextFloat() < 0.45F) {
			BlockPos spot = pos(cabinEnd - 2, 1, 0);
			if (box.isInside(spot)) {
				EndVillager castaway = ModEntities.END_VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (castaway != null) {
					castaway.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.0F, 0.0F);
					castaway.setProfession(EndVillager.Profession.CASTAWAY);
					castaway.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.STRUCTURE, null);
					castaway.setPersistenceRequired();
					castaway.setHomeTo(spot, 2);
					level.addFreshEntityWithPassengers(castaway);
				}
			}
		}
	}

	private Direction towardsBow() {
		return alongX ? (flip > 0 ? Direction.EAST : Direction.WEST) : (flip > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	private void buildHold(WorldGenLevel level, BoundingBox box, RandomSource random, int cabinEnd) {
		int hatch = Math.max(cabinEnd + 3, type == BROKEN ? gapAt - 2 : 1);
		BlockState stable = ModBlocks.ASTRAL_PLANKS.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int v = -1; v <= 1; v++) {
			put(level, box, pos(hatch, 0, v), air);
			put(level, box, pos(hatch - 1, 0, v), stable);
		}
		int bottom = -keelDepth(hatch - 1, 0);
		for (int w = bottom; w <= 0; w++) {
			put(level, box, pos(hatch - 2, w, 0), stable);
		}
		for (int w = bottom + 1; w <= 0; w++) {
			put(level, box, pos(hatch - 1, w, 0), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, towardsBow()));
		}
		int chests = type == FREIGHTER ? 3 : 1 + random.nextInt(2);
		BlockState barrel = Blocks.BARREL.defaultBlockState();
		for (int u = cabinEnd + 1; u < halfLength - 1; u++) {
			for (int v = -beam; v <= beam; v++) {
				if (Math.abs(v) >= halfWidth(u) || (u >= hatch - 2 && u <= hatch && v == 0)) {
					continue;
				}
				int floor = -keelDepth(u, v) + 1;
				if (floor >= 0) {
					continue;
				}
				float roll2 = random.nextFloat();
				if (roll2 < 0.05F && chests > 0) {
					chests--;
					chest(level, box, pos(u, floor, v));
				} else if (roll2 < 0.28F) {
					put(level, box, pos(u, floor, v), barrel);
				}
			}
		}
		if (chests > 0) {
			chest(level, box, pos(hatch + 1, -keelDepth(hatch + 1, 0) + 1, 0));
		}
	}

	private void buildMast(WorldGenLevel level, BoundingBox box, RandomSource random, int u, int height) {
		BlockState log = ModBlocks.ASTRAL_LOG.defaultBlockState();
		BlockState yard = ModBlocks.ASTRAL_FENCE.defaultBlockState();
		int bottom = -keelDepth(u, 0) + 1;
		for (int w = bottom; w <= height; w++) {
			put(level, box, pos(u, w, 0), log);
		}
		int span = beam + 1;
		for (int v = -span; v <= span; v++) {
			put(level, box, pos(u, height - 1, v), yard);
			for (int w = 3; w < height - 1; w++) {
				float rip = (height - w) * 0.03F + Math.abs(v) * 0.08F;
				if (v != 0 && random.nextFloat() > rip) {
					put(level, box, pos(u + 1, w, v), sail());
				}
			}
		}
		buildBalloon(level, box, random, u, height);
	}

	/** The crystal that keeps this end of the ship in the air, chained to the deck. */
	private void buildBalloon(WorldGenLevel level, BoundingBox box, RandomSource random, int u, int mastTop) {
		BlockState crystal = ModBlocks.PRISM_CRYSTAL_BLOCK.defaultBlockState();
		BlockState lamp = ModBlocks.PRISM_LAMP.defaultBlockState();
		BlockState chain = Blocks.IRON_CHAIN.defaultBlockState();
		int centre = mastTop + 6;
		int across = beam + (type == CAPSIZED ? 0 : 1);
		for (int du = -4; du <= 4; du++) {
			for (int v = -across; v <= across; v++) {
				for (int dw = -3; dw <= 3; dw++) {
					float shape = du * du / 16.0F + v * v / (float) (across * across) + dw * dw / 9.0F;
					if (shape <= 1.0F && (type != CAPSIZED || random.nextFloat() > 0.3F)) {
						put(level, box, pos(u + du, centre + dw, v), random.nextFloat() < 0.12F ? lamp : crystal);
					}
				}
			}
		}
		for (int v = -beam + 1; v <= beam - 1; v += Math.max(1, 2 * (beam - 1))) {
			for (int w = 1; w <= centre - 3; w++) {
				put(level, box, pos(u - 2, w, v), chain);
			}
		}
	}

	/** A plank walk of rotten boards across the break, carrying a player only if they hurry. */
	private void buildGangway(WorldGenLevel level, BoundingBox box) {
		BlockState rotten = ModBlocks.CRUMBLING_PLANKS.defaultBlockState();
		int start = gapAt - 1;
		int length = gapLength + 3;
		for (int i = 0; i <= length; i++) {
			int w = -Math.round(gapDrop * i / (float) length);
			put(level, box, rawPos(start + i, w, 0), rotten);
		}
	}

	private void chest(WorldGenLevel level, BoundingBox box, BlockPos pos) {
		if (box.isInside(pos)) {
			level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
			RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(seed ^ pos.asLong()), pos, LOOT);
		}
	}
}
