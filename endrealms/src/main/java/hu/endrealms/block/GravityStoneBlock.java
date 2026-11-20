package hu.endrealms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Gravity Stone: while powered by redstone, everything within six blocks falls slowly and jumps high. Lifts, jump
 * courses and fall-safe zones are built with it; it only ever helps, so it is safe to leave on.
 */
public class GravityStoneBlock extends Block {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final double RADIUS = 6.0;
	private static final int STEP = 10;
	private static final int EFFECT_TICKS = 30;

	public GravityStoneBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean movedByPiston) {
		if (level.isClientSide()) {
			return;
		}
		boolean powered = level.hasNeighborSignal(pos);
		if (powered != state.getValue(POWERED)) {
			level.setBlock(pos, state.setValue(POWERED, powered), 3);
			level.playSound(null, pos, powered ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (state.getValue(POWERED) && !oldState.is(this) || state.getValue(POWERED) && !oldState.getValue(POWERED)) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(POWERED)) {
			return;
		}
		Vec3 centre = Vec3.atCenterOf(pos);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS),
				entity -> entity.isAlive() && entity.position().distanceToSqr(centre) <= RADIUS * RADIUS)) {
			entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EFFECT_TICKS, 0, true, false, true));
			entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, EFFECT_TICKS, 2, true, false, true));
		}
		level.scheduleTick(pos, this, STEP);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(POWERED)) {
			return;
		}
		for (int i = 0; i < 3; i++) {
			double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * RADIUS * 1.6;
			double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * RADIUS * 1.6;
			double y = pos.getY() + random.nextDouble() * 3.0;
			level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.03 + random.nextDouble() * 0.04, 0.0);
		}
	}
}
