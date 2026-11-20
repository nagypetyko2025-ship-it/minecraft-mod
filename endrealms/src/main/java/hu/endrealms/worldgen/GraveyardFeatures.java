package hu.endrealms.worldgen;

import com.mojang.serialization.MapCodec;
import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;
import hu.endrealms.entity.BoneDrake;
import hu.endrealms.registry.ModEntities;
import net.minecraft.world.entity.EntitySpawnReason;

/** Dragon Graveyard: pale ground, giant ribcages and skulls lying half in it, and trees grown out of bone. */
public final class GraveyardFeatures {
	private static final int FLAGS = Block.UPDATE_CLIENTS;
	private static final ResourceKey<LootTable> TREASURE = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/dragon_graveyard"));

	private GraveyardFeatures() {
	}

	private static BlockState bone() {
		return Blocks.BONE_BLOCK.defaultBlockState();
	}

	private static int ground(WorldGenLevel level, int x, int z) {
		return level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
	}

	private static boolean onLand(WorldGenLevel level, int x, int z, int y, int tolerance) {
		int height = ground(level, x, z);
		return height > level.getMinY() + 4 && Math.abs(height - y) <= tolerance;
	}

	private static void chest(WorldGenLevel level, RandomSource random, BlockPos pos, Direction facing) {
		level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, facing), FLAGS);
		RandomizableContainer.setBlockEntityLootTable(level, random, pos, TREASURE);
	}

	/** Turns the top of the end stone into Bone Soil, with patches of bare bone. */
	public static final class Blanket implements Feature {
		public static final Blanket INSTANCE = new Blanket();
		public static final MapCodec<Blanket> CODEC = MapCodec.unit(INSTANCE);

		private Blanket() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			int chunkX = origin.getX() & ~15;
			int chunkZ = origin.getZ() & ~15;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			BlockState soil = ModBlocks.BONE_SOIL.defaultBlockState();
			for (int x = chunkX; x < chunkX + 16; x++) {
				for (int z = chunkZ; z < chunkZ + 16; z++) {
					int top = ground(level, x, z) - 1;
					pos.set(x, top, z);
					if (!level.getBiome(pos).is(ModWorldgen.DRAGON_GRAVEYARD)) {
						continue;
					}
					float patch = BasinSites.noise(x, z, 11, 81) + 0.35F * BasinSites.noise(x, z, 4, 82);
					for (int y = top; y > top - 3 && level.getBlockState(pos.set(x, y, z)).is(Blocks.END_STONE); y--) {
						level.setBlock(pos, y == top && patch > 0.62F ? bone() : soil, FLAGS);
					}
				}
			}
			return true;
		}
	}

	/** A spine lying along the ground on a low arch, with pairs of ribs curving down into it on both sides. */
	public static final class Ribcage implements Feature {
		public static final Ribcage INSTANCE = new Ribcage();
		public static final MapCodec<Ribcage> CODEC = MapCodec.unit(INSTANCE);

		private Ribcage() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			boolean alongX = random.nextBoolean();
			int half = 10 + random.nextInt(5);
			int y0 = origin.getY();
			for (int t = -half; t <= half; t += half) {
				int x = origin.getX() + (alongX ? t : 0);
				int z = origin.getZ() + (alongX ? 0 : t);
				if (!onLand(level, x, z, y0, 6)) {
					return false;
				}
			}
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int t = -half; t <= half; t++) {
				float along = t / (float) half;
				int spine = y0 + Math.round(5.0F + 4.0F * (1.0F - along * along));
				put(level, pos, origin, alongX, t, 0, spine);
				if (Math.floorMod(t, 3) == 0 && Math.abs(t) < half - 1) {
					float radius = 4.0F + 5.0F * (1.0F - along * along);
					for (int side = -1; side <= 1; side += 2) {
						rib(level, pos, origin, alongX, t, side, spine, radius);
					}
				}
			}
			if (random.nextInt(3) == 0) {
				BlockPos spot = new BlockPos(origin.getX(), ground(level, origin.getX(), origin.getZ()), origin.getZ());
				chest(level, random, spot, alongX ? Direction.NORTH : Direction.EAST);
			}
			return true;
		}

		private static void rib(WorldGenLevel level, BlockPos.MutableBlockPos pos, BlockPos origin, boolean alongX, int t, int side, int spine, float radius) {
			for (float angle = 0.0F; angle < Mth.PI * 0.9F; angle += 0.09F) {
				int across = side * Math.round(Mth.sin(angle) * radius);
				int y = spine - Math.round((1.0F - Mth.cos(angle)) * radius);
				int x = origin.getX() + (alongX ? t : across);
				int z = origin.getZ() + (alongX ? across : t);
				if (y < ground(level, x, z) - 1) {
					return;
				}
				put(level, pos, origin, alongX, t, across, y);
			}
		}

		private static void put(WorldGenLevel level, BlockPos.MutableBlockPos pos, BlockPos origin, boolean alongX, int t, int across, int y) {
			level.setBlock(pos.set(origin.getX() + (alongX ? t : across), y, origin.getZ() + (alongX ? across : t)), bone(), FLAGS);
		}
	}

	/** A hollow skull you can walk into through its open jaws, with a chest at the back of the cranium. */
	public static final class Skull implements Feature {
		public static final Skull INSTANCE = new Skull();
		public static final MapCodec<Skull> CODEC = MapCodec.unit(INSTANCE);

		private Skull() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			Direction forward = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			Direction side = forward.getClockWise();
			int y0 = origin.getY();
			for (int a = -7; a <= 13; a += 10) {
				if (!onLand(level, origin.getX() + forward.getStepX() * a, origin.getZ() + forward.getStepZ() * a, y0, 4)) {
					return false;
				}
			}
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int a = -7; a <= 13; a++) {
				for (int b = -6; b <= 6; b++) {
					for (int h = 0; h <= 10; h++) {
						float cranium = Mth.square(a / 7.0F) + Mth.square(b / 6.0F) + Mth.square((h - 5) / 5.0F);
						float snout = Mth.square((a - 8) / 5.0F) + Mth.square(b / 3.5F) + Mth.square((h - 3) / 3.0F);
						boolean inside = cranium < 0.62F || snout < 0.5F;
						boolean shell = !inside && (cranium <= 1.0F || snout <= 1.0F);
						boolean opening = (a >= 11 && Math.abs(b) <= 2 && h >= 1 && h <= 3)
								|| (Math.abs(a - 3) <= 1 && Math.abs(Math.abs(b) - 5) <= 1 && h >= 5 && h <= 6);
						pos.set(origin.getX() + forward.getStepX() * a + side.getStepX() * b, y0 + h, origin.getZ() + forward.getStepZ() * a + side.getStepZ() * b);
						if (shell && !opening) {
							level.setBlock(pos, bone(), FLAGS);
						} else if ((inside || opening) && h >= 1 && (inside ? cranium <= 1.0F || snout <= 1.0F : shell)) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
						}
					}
				}
			}
			for (int b = -3; b <= 3; b += 2) {
				pos.set(origin.getX() + forward.getStepX() * 12 + side.getStepX() * b, y0 + 4, origin.getZ() + forward.getStepZ() * 12 + side.getStepZ() * b);
				level.setBlock(pos, bone(), FLAGS);
			}
			BlockPos spot = new BlockPos(origin.getX() - forward.getStepX() * 3, y0 + 1, origin.getZ() - forward.getStepZ() * 3);
			chest(level, random, spot, forward);
			return true;
		}
	}

	/** A pale trunk that splits into bony branches, each ending in a faint light. */
	public static final class BoneTree implements Feature {
		public static final BoneTree INSTANCE = new BoneTree();
		public static final MapCodec<BoneTree> CODEC = MapCodec.unit(INSTANCE);

		private BoneTree() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			if (!level.getBlockState(origin.below()).is(ModBlocks.BONE_SOIL)) {
				return false;
			}
			int height = 6 + random.nextInt(5);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int y = 0; y < height; y++) {
				level.setBlock(pos.setWithOffset(origin, 0, y, 0), bone(), FLAGS);
			}
			int branches = 3 + random.nextInt(3);
			float start = random.nextFloat() * Mth.TWO_PI;
			for (int i = 0; i < branches; i++) {
				float angle = start + i * Mth.TWO_PI / branches + (random.nextFloat() - 0.5F) * 0.5F;
				int length = 3 + random.nextInt(4);
				int baseY = height - 1 - random.nextInt(3);
				int x = 0;
				int z = 0;
				int y = 0;
				for (int s = 1; s <= length; s++) {
					x = Math.round(Mth.cos(angle) * s);
					z = Math.round(Mth.sin(angle) * s);
					y = Math.round(s * 0.7F);
					level.setBlock(pos.setWithOffset(origin, x, baseY + y, z), bone(), FLAGS);
				}
				level.setBlock(pos.setWithOffset(origin, x, baseY + y + 1, z), Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP), FLAGS);
			}
			return true;
		}
	}

	/**
	 * A nest of bone at the very centre of each graveyard island, with a wild Bone Drake sitting in it. Runs in every
	 * graveyard chunk but builds only in the one holding the centre, after the ribcages, skulls and trees.
	 */
	public static final class DrakeNest implements Feature {
		public static final DrakeNest INSTANCE = new DrakeNest();
		public static final MapCodec<DrakeNest> CODEC = MapCodec.unit(INSTANCE);
		private static final int RADIUS = 4;

		private DrakeNest() {
		}

		@Override
		public MapCodec<? extends Feature> codec() {
			return CODEC;
		}

		@Override
		public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
			GraveyardIslandFeature.Sites.Site site = GraveyardIslandFeature.Sites.siteFor(origin.getX(), origin.getZ());
			if (site == null || site.x() >> 4 != origin.getX() >> 4 || site.z() >> 4 != origin.getZ() >> 4) {
				return false;
			}
			int cx = site.x();
			int cz = site.z();
			int y = ground(level, cx, cz);
			if (y <= level.getMinY() + 4) {
				return false;
			}
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			BlockState air = Blocks.AIR.defaultBlockState();
			BlockState dragonBone = ModBlocks.DRAGON_BONE_BLOCK.defaultBlockState();
			BlockState soil = ModBlocks.BONE_SOIL.defaultBlockState();
			for (int dx = -RADIUS - 1; dx <= RADIUS + 1; dx++) {
				for (int dz = -RADIUS - 1; dz <= RADIUS + 1; dz++) {
					float d = Mth.sqrt(dx * dx + dz * dz);
					if (d > RADIUS + 0.7F) {
						continue;
					}
					int x = cx + dx;
					int z = cz + dz;
					for (int up = 0; up <= 7; up++) {           // a clear space, whatever bones lay here
						level.setBlock(pos.set(x, y + up, z), air, FLAGS);
					}
					for (int down = 1; down <= 3; down++) {      // and solid ground under the nest
						level.setBlock(pos.set(x, y - down, z), soil, FLAGS);
					}
					if (d > RADIUS - 0.6F) {                     // the rim: bone, two high, with gaps
						if (random.nextInt(5) != 0) {
							level.setBlock(pos.set(x, y, z), random.nextInt(3) == 0 ? dragonBone : bone(), FLAGS);
							if (random.nextInt(3) != 0) {
								level.setBlock(pos.set(x, y + 1, z), bone(), FLAGS);
							}
						}
					} else if (d < 2.0F) {
						level.setBlock(pos.set(x, y - 1, z), dragonBone, FLAGS);
					}
				}
			}
			BoneDrake drake = ModEntities.BONE_DRAKE.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (drake != null) {
				drake.snapTo(cx + 0.5, y, cz + 0.5, random.nextFloat() * 360.0F, 0.0F);
				drake.setPersistenceRequired();
				drake.setInSittingPose(true);
				level.addFreshEntityWithPassengers(drake);
			}
			return true;
		}
	}
}
