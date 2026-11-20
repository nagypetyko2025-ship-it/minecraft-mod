package hu.endrealms.block;

import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Starreed: bamboo-like cane of the Astral Forest. Stacks up to 5 blocks on end soil and grows slowly by itself.
 * By hand a cane gives Starberries, with shears the cane itself. A Starberry planted on End Soil is a shoot
 * ({@link #GROWN} false) that turns into a cane after a while, so berries are farmed, not copied.
 */
public class StarreedBlock extends Block {
	public static final BooleanProperty TOP = BooleanProperty.create("top");
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
	/** False for a shoot just planted from a Starberry. Canes from before shoots existed load as grown. */
	public static final BooleanProperty GROWN = BooleanProperty.create("grown");
	public static final int MAX_HEIGHT = 5;
	private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);
	private static final VoxelShape SHOOT_SHAPE = Block.column(6.0, 0.0, 9.0);

	public StarreedBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(TOP, true).setValue(AGE, 0).setValue(GROWN, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TOP, AGE, GROWN);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(GROWN) ? SHAPE : SHOOT_SHAPE;
	}

	/** A shoot, as a Starberry plants it. */
	public static BlockState shoot() {
		return ModBlocks.STARREED.defaultBlockState().setValue(GROWN, false);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(this) && below.getValue(GROWN) || below.is(ModBlocks.END_SOIL);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(TOP, !context.getLevel().getBlockState(context.getClickedPos().above()).is(this));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (!state.canSurvive(level, pos)) {
			ticks.scheduleTick(pos, this, 1);   // breaks next tick, so that each block of a falling column drops its loot
		}
		return direction == Direction.UP ? state.setValue(TOP, !neighborState.is(this)) : state;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.canSurvive(level, pos)) {
			level.destroyBlock(pos, true);
		}
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(GROWN)) {                // a shoot first becomes a cane, on the same slow clock
			int age = state.getValue(AGE);
			level.setBlock(pos, age < 7 ? state.setValue(AGE, age + 1) : state.setValue(AGE, 0).setValue(GROWN, true), Block.UPDATE_CLIENTS);
			return;
		}
		if (!state.getValue(TOP) || !level.getBlockState(pos.above()).isAir()) {
			return;
		}
		int height = 1;
		while (height < MAX_HEIGHT && level.getBlockState(pos.below(height)).is(this)) {
			height++;
		}
		if (height >= MAX_HEIGHT) {
			return;
		}
		int age = state.getValue(AGE);
		if (age < 7) {
			level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
		} else {
			level.setBlockAndUpdate(pos.above(), this.defaultBlockState());
			level.setBlock(pos, state.setValue(AGE, 0).setValue(TOP, false), Block.UPDATE_CLIENTS);
		}
	}
}
