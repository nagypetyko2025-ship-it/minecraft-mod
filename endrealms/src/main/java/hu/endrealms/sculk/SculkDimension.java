package hu.endrealms.sculk;

import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The fourth dimension: a dead, dark copy of a surface world where the sculk has taken everything (data/endrealms/
 * dimension/sculk.json). Its monsters are meant to be as strong as Chorusite gear can handle.
 * <p>
 * The way in, for now, is a Sculk Portal block (also put down by /astra sculk portal:summon). Travel is one to one:
 * you come out at the same x and z on the other side, next to a portal leading back, made there if there is none near.
 */
public final class SculkDimension {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, EndRealms.id("sculk"));
	/** How far around the arrival point an existing portal is looked for before a new one is made. */
	private static final int SEARCH = 6;

	private SculkDimension() {
	}

	private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> BARRENS =
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, EndRealms.id("sculk_barrens"));
	/** How far out the search for open Silent Barrens goes, and how high its ground may lie (no mountain tops). */
	private static final int BARRENS_SEARCH = 4000;
	private static final int BARRENS_STEP = 48;
	private static final int MAX_GROUND = 100;

	/**
	 * Where a portal in {@code from} leads. Into the Sculk dimension: the portal linked to this one, or else a new one
	 * on open ground in the nearest Silent Barrens. Out of it: back to the Overworld portal it was reached from, or, for
	 * a portal made in the Sculk dimension itself, the same spot in the Overworld.
	 */
	static TeleportTransition destination(ServerLevel from, Entity entity, BlockPos through) {
		net.minecraft.server.MinecraftServer server = from.getServer();
		boolean leaving = from.dimension() == LEVEL;
		ServerLevel to = leaving ? server.overworld() : server.getLevel(LEVEL);
		if (to == null) {
			return null;                // the dimension is missing (an old world without its data)
		}
		SculkLinks links = SculkLinks.get(server);
		BlockPos portal = null;
		if (leaving) {
			BlockPos home = links.overworldFor(through);
			if (home != null) {
				portal = portalNear(to, home);
			}
			if (portal == null) {
				int x = home != null ? home.getX() : net.minecraft.util.Mth.floor(entity.getX());
				int z = home != null ? home.getZ() : net.minecraft.util.Mth.floor(entity.getZ());
				portal = findPortal(to, x, z);
				if (portal == null) {
					portal = makePortal(to, x, z);
				}
				if (home == null) {
					links.link(portal, through);
				}
			}
		} else {
			BlockPos there = links.sculkFor(through);
			if (there != null) {
				portal = portalNear(to, there);
			}
			if (portal == null) {
				BlockPos ground = barrensNear(to, through.getX(), through.getZ());
				portal = findPortal(to, ground.getX(), ground.getZ());
				if (portal == null) {
					portal = makePortal(to, ground.getX(), ground.getZ());
				}
				links.link(through, portal);
			}
		}
		BlockPos stand = standingSpotBeside(to, portal);
		EndRealms.LOGGER.debug("Sculk portal: {} at {} -> {} portal {}, arriving at {}", entity.getName().getString(), through,
				to.dimension().identifier(), portal, stand);
		return new TeleportTransition(to, Vec3.atBottomCenterOf(stand), Vec3.ZERO, entity.getYRot(), entity.getXRot(),
				arrived -> to.playSound(null, arrived.getX(), arrived.getY(), arrived.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.0F, 0.6F));
	}

	/** A portal block at or right next to {@code pos}, if it is still there. */
	private static BlockPos portalNear(ServerLevel level, BlockPos pos) {
		level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = -3; dy <= 3; dy++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (level.getBlockState(p.setWithOffset(pos, dx, dy, dz)).is(ModBlocks.SCULK_PORTAL)) {
						return p.immutable();
					}
				}
			}
		}
		return null;
	}

	/**
	 * The nearest open ground of the Silent Barrens: searched in widening rings from the arrival point, by the terrain
	 * the generator would make (nothing is generated for it), taking the first spot whose surface is Silent Barrens and
	 * not up a mountain. Falls back to the arrival point itself.
	 */
	private static BlockPos barrensNear(ServerLevel level, int x, int z) {
		net.minecraft.world.level.chunk.ChunkGenerator generator = level.getChunkSource().getGenerator();
		net.minecraft.world.level.levelgen.RandomState random = level.getChunkSource().randomState();
		net.minecraft.world.level.biome.BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(random);
		for (int r = 0; r <= BARRENS_SEARCH; r += BARRENS_STEP) {
			int points = Math.max(1, (int) (2 * Math.PI * r / BARRENS_STEP));
			for (int i = 0; i < points; i++) {
				double angle = 2 * Math.PI * i / points;
				int px = x + (int) Math.round(Math.cos(angle) * r);
				int pz = z + (int) Math.round(Math.sin(angle) * r);
				int ground = generator.getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, level, random);
				if (ground < level.getSeaLevel() - 8 || ground > MAX_GROUND) {
					continue;
				}
				var biome = biomes.getNoiseBiome(px >> 2, ground >> 2, pz >> 2);
				if (biome.is(BARRENS)) {
					return new BlockPos(px, ground, pz);
				}
			}
		}
		return new BlockPos(x, 64, z);
	}

	private static BlockPos findPortal(ServerLevel level, int x, int z) {
		int y = surface(level, x, z);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -SEARCH; dx <= SEARCH; dx++) {
			for (int dz = -SEARCH; dz <= SEARCH; dz++) {
				for (int dy = -SEARCH; dy <= SEARCH; dy++) {
					if (level.getBlockState(pos.set(x + dx, y + dy, z + dz)).is(ModBlocks.SCULK_PORTAL)) {
						return pos.immutable();
					}
				}
			}
		}
		return null;
	}

	/**
	 * A new portal sunk into the ground at the arrival point, set in a ring of sculk; over water or lava a small
	 * deepslate platform carries it.
	 */
	private static BlockPos makePortal(ServerLevel level, int x, int z) {
		int y = surface(level, x, z);
		BlockPos portal = new BlockPos(x, y - 1, z);
		boolean wet = !level.getBlockState(portal).getFluidState().isEmpty();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos around = portal.offset(dx, 0, dz);
				if (wet || level.getBlockState(around).isAir() || !level.getBlockState(around).getFluidState().isEmpty()) {
					level.setBlockAndUpdate(around, Blocks.DEEPSLATE_TILES.defaultBlockState());
				} else if (dx != 0 || dz != 0) {
					level.setBlockAndUpdate(around, Blocks.SCULK.defaultBlockState());
				}
				clear(level, around.above());
				clear(level, around.above(2));
			}
		}
		level.setBlockAndUpdate(portal.below(), Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
		level.setBlockAndUpdate(portal, ModBlocks.SCULK_PORTAL.defaultBlockState());
		return portal;
	}

	private static void clear(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.isAir() && !state.is(ModBlocks.SCULK_PORTAL)) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
	}

	/** Next to the portal, not in it, so nobody is sent straight back. */
	private static BlockPos standingSpotBeside(ServerLevel level, BlockPos portal) {
		for (int r = 1; r <= 2; r++) {                  // the four sides first, then a little further out
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r || (r == 1 && dx != 0 && dz != 0)) {
						continue;
					}
					for (int dy = 0; dy <= 2; dy++) {
						BlockPos ground = portal.offset(dx, dy, dz);
						BlockPos feet = ground.above();
						if (!level.getBlockState(ground).getCollisionShape(level, ground).isEmpty()
								&& level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
								&& level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
								&& level.getBlockState(feet).getFluidState().isEmpty()) {
							return feet;
						}
					}
				}
			}
		}
		return portal.above();     // walled in all round: on the portal itself, the cooldown gives time to step off
	}

	private static int surface(ServerLevel level, int x, int z) {
		level.getChunk(x >> 4, z >> 4);           // generate it first, so the height is real
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return Math.max(level.getMinY() + 2, Math.min(y, level.getMinY() + level.getLogicalHeight() - 3));
	}

	/** For the debug command: a portal right under the player's feet. */
	public static void summonPortal(ServerLevel level, BlockPos feet) {
		level.setBlockAndUpdate(feet.below(), ModBlocks.SCULK_PORTAL.defaultBlockState());
		level.playSound(null, feet, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 0.6F, 0.7F);
	}
}
