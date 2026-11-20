package hu.endrealms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Rotten deck of the Sky Wrecks: a player's weight sets it creaking, a few seconds later it drops, often taking its neighbours along. */
public class CrumblingPlanksBlock extends Block {
	public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");
	private static final int FIRST_DELAY = 50;

	public CrumblingPlanksBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TRIGGERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TRIGGERED);
	}

	private void trigger(Level level, BlockPos pos, BlockState state, int delay) {
		level.setBlock(pos, state.setValue(TRIGGERED, true), 3);
		level.scheduleTick(pos, this, delay);
		level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.6F, 0.6F);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		if (!level.isClientSide() && entity instanceof Player && !state.getValue(TRIGGERED)) {
			trigger(level, pos, state, FIRST_DELAY);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(TRIGGERED)) {
			return;
		}
		FallingBlockEntity.fall(level, pos, state.setValue(TRIGGERED, false)).disableDrop();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos next = pos.relative(side);
			BlockState neighbour = level.getBlockState(next);
			if (neighbour.is(this) && !neighbour.getValue(TRIGGERED) && random.nextFloat() < 0.55F) {
				trigger(level, next, neighbour, 12 + random.nextInt(25));
			}
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(TRIGGERED) && random.nextInt(2) == 0) {
			level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0.0, -0.05, 0.0);
		}
	}
}
