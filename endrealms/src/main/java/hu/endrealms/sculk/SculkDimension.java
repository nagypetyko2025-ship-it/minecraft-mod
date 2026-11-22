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

	/** Where a portal in {@code from} leads: the Sculk dimension, or back to the Overworld from there. */
	static TeleportTransition destination(ServerLevel from, Entity entity) {
		ServerLevel to = from.dimension() == LEVEL ? from.getServer().overworld() : from.getServer().getLevel(LEVEL);
		if (to == null) {
			return null;                // the dimension is missing (an old world without its data)
		}
		int x = net.minecraft.util.Mth.floor(entity.getX());
		int z = net.minecraft.util.Mth.floor(entity.getZ());
		BlockPos portal = findPortal(to, x, z);
		if (portal == null) {
			portal = makePortal(to, x, z);
		}
		BlockPos stand = standingSpotBeside(to, portal);
		return new TeleportTransition(to, Vec3.atBottomCenterOf(stand), Vec3.ZERO, entity.getYRot(), entity.getXRot(),
				arrived -> to.playSound(null, arrived.getX(), arrived.getY(), arrived.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.0F, 0.6F));
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
