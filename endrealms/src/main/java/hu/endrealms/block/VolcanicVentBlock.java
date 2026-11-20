package hu.endrealms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

/**
 * Volcanic Vent: smoulders and burns what stands next to it, and every so often erupts in a column of smoke and sparks
 * that reaches further. The eruption is only an effect plus the burn; it throws nothing. A redstone signal or a solid
 * block on top shuts it off, which is what makes it usable as a trap.
 */
public class VolcanicVentBlock extends Block {
	public static final BooleanProperty ERUPTING = BooleanProperty.create("erupting");
	private static final int IDLE_STEP = 20;
	private static final int ERUPT_STEP = 5;

	public VolcanicVentBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ERUPTING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ERUPTING);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			level.scheduleTick(pos, this, IDLE_STEP);
		}
	}

	/** Worldgen and chunk restores can lose the running tick; a random tick starts it again. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
			level.scheduleTick(pos, this, IDLE_STEP);
		}
	}

	private static boolean blocked(ServerLevel level, BlockPos pos) {
		return level.hasNeighborSignal(pos) || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
	}

	private static void burn(ServerLevel level, BlockPos pos, double reach, double height, float damage) {
		AABB area = new AABB(pos.getX() + 0.5 - reach, pos.getY() + 1.0, pos.getZ() + 0.5 - reach, pos.getX() + 0.5 + reach, pos.getY() + 1.0 + height, pos.getZ() + 0.5 + reach);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (!entity.fireImmune() && entity.hurtServer(level, level.damageSources().hotFloor(), damage)) {
				entity.igniteForSeconds(3.0F);
			}
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		boolean erupting = state.getValue(ERUPTING);
		if (blocked(level, pos)) {
			if (erupting) {
				level.setBlock(pos, state.setValue(ERUPTING, false), 3);
			}
			level.scheduleTick(pos, this, IDLE_STEP);
			return;
		}
		if (!erupting) {
			burn(level, pos, 1.2, 2.0, 0.5F);
			if (random.nextFloat() < 0.12F) {
				level.setBlock(pos, state.setValue(ERUPTING, true), 3);
				level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.4F, 0.6F);
				level.scheduleTick(pos, this, ERUPT_STEP);
			} else {
				level.scheduleTick(pos, this, IDLE_STEP);
			}
			return;
		}
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 1.1;
		double z = pos.getZ() + 0.5;
		level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y, z, 4, 0.25, 0.1, 0.25, 0.03);
		level.sendParticles(ParticleTypes.LAVA, x, y, z, 5, 0.3, 0.2, 0.3, 0.0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.0, z, 18, 0.5, 1.2, 0.5, 0.08);
		level.sendParticles(ParticleTypes.FLAME, x, y, z, 10, 0.3, 0.6, 0.3, 0.04);
		burn(level, pos, 3.0, 5.0, 1.5F);
		if (random.nextFloat() < 0.07F) {
			level.setBlock(pos, state.setValue(ERUPTING, false), 3);
			level.scheduleTick(pos, this, IDLE_STEP);
		} else {
			level.scheduleTick(pos, this, ERUPT_STEP);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (level.getBlockState(pos.above()).isAir()) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.04, 0.0);
		}
	}
}
