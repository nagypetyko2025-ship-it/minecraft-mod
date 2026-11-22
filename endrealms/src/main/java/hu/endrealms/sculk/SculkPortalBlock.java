package hu.endrealms.sculk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A pool of living darkness in the ground: step in, stand still for two seconds, and you are on the other side
 * ({@link SculkDimension}). It has no collision, so it is walked into from above like a hole.
 */
public class SculkPortalBlock extends Block implements Portal {
	/** Most of a block, so it sits a little below the ground around it. */
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
	private static final int WAIT_TICKS = 40;

	public SculkPortalBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
		if (entity.canUsePortal(false)) {
			entity.setAsInsidePortal(this, pos);
		}
	}

	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity) {
		return entity instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().invulnerable ? 1 : WAIT_TICKS;
	}

	@Override
	public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
		return SculkDimension.destination(level, entity);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + random.nextDouble(), pos.getY() + 0.8, pos.getZ() + random.nextDouble(),
					0.0, 0.02 + random.nextDouble() * 0.03, 0.0);
		}
		if (random.nextInt(60) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS,
					0.5F, 0.5F + random.nextFloat() * 0.2F, false);
		}
	}
}
