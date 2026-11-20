package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModStructures;
import hu.endrealms.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;

/** The Scorched Barrens' small volcanoes, glowing ground cracks and blanket of ash. */
public final class ScorchedFeatures {
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	private ScorchedFeatures() {
	}

	private static boolean scorched(BlockState state) {
		return state.is(ModBlocks.SCORCHED_END_STONE);
	}

	private static int ground(WorldGenLevel level, int x, int z) {
		return level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
	}

	/** Volcanoes keep off the Crypts: a cone on top of the building or its stairs would bury them. */
	private static boolean nearCrypt(WorldGenLevel level, BlockPos origin) {
		Structure crypt = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(ModStructures.CRYPT_KEY);
		if (crypt == null) {
			return false;
		}
		for (int dx = -16; dx <= 16; dx += 16) {
			for (int dz = -16; dz <= 16; dz += 16) {
				if (!level.getChunk(origin.offset(dx, 0, dz)).getReferencesForStructure(crypt).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	/** A cone of scorched stone with a bowl of ember rock at the top and glowing streams down its flanks. */
	public static final class Volcano implements Feature {
		public static final Volcano INSTANCE = new Volcano();
		public static final MapCodec<Volcano> CODEC = MapCodec.unit(INSTANCE);

		private Volcano() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			if (!scorched(level.getBlockState(origin.below())) || nearCrypt(level, origin)) {
				return false;
			}
			int radius = 4 + random.nextInt(6);
			int height = radius + 1 + random.nextInt(3);
			float crater = Math.max(1.3F, radius * 0.3F);
			int depth = 1 + random.nextInt(2);
			int base = origin.getY();
			for (int[] o : new int[][] {{-radius, 0}, {radius, 0}, {0, -radius}, {0, radius}}) {
				if (ground(level, origin.getX() + o[0], origin.getZ() + o[1]) < base - 6) {
					return false;                  // would hang over the island's edge
				}
			}
			int streams = 2 + random.nextInt(3);
			float[] streamAngles = new float[streams];
			for (int i = 0; i < streams; i++) {
				streamAngles[i] = random.nextFloat() * Mth.TWO_PI;
			}
			BlockState stone = ModBlocks.SCORCHED_END_STONE.defaultBlockState();
			BlockState ember = ModBlocks.EMBER_ROCK.defaultBlockState();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					float distance = Mth.sqrt(dx * dx + dz * dz);
					if (distance > radius) {
						continue;
					}
					int top;
					if (distance < crater) {
						top = base + height - Math.round(depth * (1.0F - distance * distance / (crater * crater)));
					} else {
						float t = (distance - crater) / (radius - crater);
						top = base + Math.round(height * (float) Math.pow(1.0F - t, 1.25F) + random.nextFloat() * 0.8F) - 1;
					}
					int x = origin.getX() + dx;
					int z = origin.getZ() + dz;
					int from = ground(level, x, z);
					if (from < base - 8 || from > top) {
						continue;
					}
					boolean glowing = distance < crater;
					if (!glowing && distance < radius * 0.85F) {
						float angle = (float) Mth.atan2(dz, dx);
						for (float stream : streamAngles) {
							glowing |= Math.abs(Mth.wrapDegrees((angle - stream) * Mth.RAD_TO_DEG)) * Mth.DEG_TO_RAD * distance < 0.75F;
						}
					}
					for (int y = from; y <= top; y++) {
						pos.set(x, y, z);
						if (level.getBlockState(pos).canBeReplaced() || scorched(level.getBlockState(pos))) {
							level.setBlock(pos, y == top && glowing ? ember : stone, FLAGS);
						}
					}
					if (from > top - 1) {
						pos.set(x, top, z);
						if (glowing && scorched(level.getBlockState(pos))) {
							level.setBlock(pos, ember, FLAGS);
						}
					}
				}
			}
			BlockPos heart = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, origin).below();
			if (level.getBlockState(heart).is(ModBlocks.EMBER_ROCK)) {
				level.setBlock(heart, ModBlocks.VOLCANIC_VENT.defaultBlockState(), FLAGS);
				level.scheduleTick(heart, ModBlocks.VOLCANIC_VENT, 20 + random.nextInt(100));
			}
			return true;
		}
	}

	/** Runs once per chunk, last: a few ember cracks in the ground, then ash over everything still scorched. */
	public static final class AshBlanket implements Feature {
		public static final AshBlanket INSTANCE = new AshBlanket();
		public static final MapCodec<AshBlanket> CODEC = MapCodec.unit(INSTANCE);

		private AshBlanket() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		private static boolean inBiome(WorldGenLevel level, BlockPos pos) {
			return level.getBiome(pos).is(ModWorldgen.SCORCHED_BARRENS);
		}

		/** Smooth 0..1 field that shapes bare patches and drifts; only depends on the coordinates, so it crosses chunks. */
		private static float drift(int x, int z) {
			float a = Mth.sin(x * 0.13F + Mth.sin(z * 0.07F) * 1.9F);
			float b = Mth.cos(z * 0.11F + Mth.sin(x * 0.09F) * 1.7F);
			return 0.5F + 0.5F * a * b;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			int chunkX = origin.getX() & ~15;
			int chunkZ = origin.getZ() & ~15;
			for (int crack = 2 + random.nextInt(3); crack > 0; crack--) {
				int x = chunkX + random.nextInt(16);
				int z = chunkZ + random.nextInt(16);
				for (int length = 3 + random.nextInt(6); length > 0 && x >= chunkX && x < chunkX + 16 && z >= chunkZ && z < chunkZ + 16; length--) {
					BlockPos below = new BlockPos(x, ground(level, x, z) - 1, z);
					if (scorched(level.getBlockState(below)) && inBiome(level, below)) {
						level.setBlock(below, ModBlocks.EMBER_ROCK.defaultBlockState(), FLAGS);
					}
					x += random.nextInt(3) - 1;
					z += random.nextInt(3) - 1;
				}
			}
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = chunkX; x < chunkX + 16; x++) {
				for (int z = chunkZ; z < chunkZ + 16; z++) {
					int y = ground(level, x, z);
					pos.set(x, y, z);
					if (!level.getBlockState(pos).isAir() || !inBiome(level, pos)) {
						continue;
					}
					if (!scorched(level.getBlockState(pos.below()))) {
						continue;
					}
					float field = drift(x, z) + random.nextFloat() * 0.08F;
					if (field < 0.12F) {
						continue;              // bare, charred ground
					}
					// one layer has no collision (mobs can spawn on it), thicker drifts are the exception
					int layers = field > 0.86F ? 3 : field > 0.68F ? 2 : 1;
					level.setBlock(pos, ModBlocks.ASH_LAYER.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers), FLAGS);
				}
			}
			return true;
		}
	}
}
