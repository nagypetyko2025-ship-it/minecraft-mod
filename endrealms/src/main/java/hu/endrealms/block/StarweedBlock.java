package hu.endrealms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Starweed: a tall, bushy kelp-like plant of the Astral Basin. It stacks into columns (each block stands on
 * the floor or on another starweed); the topmost block shows the frond tip. Breaking a block drops everything
 * above it, like kelp.
 */
public class StarweedBlock extends AstralWaterPlantBlock {
	public static final BooleanProperty TOP = BooleanProperty.create("top");

	public StarweedBlock(Properties properties) {
		super(Block.column(14.0, 0.0, 16.0), properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(TOP, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TOP);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(this) || super.mayPlaceOn(state, level, pos);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null) {
			return null;
		}
		return state.setValue(TOP, !context.getLevel().getBlockState(context.getClickedPos().above()).is(this));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		if (updated.is(this) && direction == Direction.UP) {
			return updated.setValue(TOP, !neighborState.is(this));
		}
		return updated;
	}
}
