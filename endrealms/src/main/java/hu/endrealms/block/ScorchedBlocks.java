package hu.endrealms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Blocks of the Scorched Barrens. */
public final class ScorchedBlocks {
	private ScorchedBlocks() {
	}

	/** Ash that stacks and behaves like snow, but never melts. */
	public static class AshLayer extends SnowLayerBlock {
		public AshLayer(Properties properties) {
			super(properties);
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		}
	}

	/** Violet-hot rock: burns whoever walks on it without sneaking, and smokes where it is open to the air. */
	public static class EmberRock extends MagmaBlock {
		public EmberRock(Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			if (random.nextInt(4) == 0 && level.getBlockState(pos.above()).isAir()) {
				double x = pos.getX() + random.nextDouble();
				double z = pos.getZ() + random.nextDouble();
				level.addParticle(ParticleTypes.LARGE_SMOKE, x, pos.getY() + 1.05, z, 0.0, 0.03, 0.0);
				if (random.nextInt(3) == 0) {
					level.addParticle(ParticleTypes.REVERSE_PORTAL, x, pos.getY() + 1.1, z, 0.0, 0.06, 0.0);
				}
			}
		}
	}
}
