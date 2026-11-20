package hu.endrealms.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * Two canvases side by side, 32 x 16 pixels. The left block holds the paint; the right one is only its other half and
 * goes when the left one goes, like the two halves of a bed.
 */
public class LargeCanvasBlock extends CanvasBlock {
	public static final BooleanProperty EXTENSION = BooleanProperty.create("extension");

	public LargeCanvasBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(EXTENSION, false));
	}

	public static boolean isExtension(BlockState state) {
		return state.getBlock() instanceof LargeCanvasBlock && state.getValue(EXTENSION);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(EXTENSION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		BlockPos other = context.getClickedPos().relative(right(state.getValue(FACING)));
		return context.getLevel().getBlockState(other).canBeReplaced(context) ? state : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.relative(right(state.getValue(FACING))), state.setValue(EXTENSION, true), 3);
		super.setPlacedBy(level, pos, state, placer, stack);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(EXTENSION) ? null : super.newBlockEntity(pos, state);
	}

	/** Each half disappears when the other one is gone. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		Direction toPartner = state.getValue(EXTENSION) ? right(state.getValue(FACING)).getOpposite() : right(state.getValue(FACING));
		if (direction == toPartner && !(neighbor.is(this) && neighbor.getValue(FACING) == state.getValue(FACING) && neighbor.getValue(EXTENSION) != state.getValue(EXTENSION))) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
	}

	/** Breaking the right half drops the painting from the left half. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && state.getValue(EXTENSION)) {
			BlockPos main = mainPos(state, pos);
			BlockState mainState = level.getBlockState(main);
			if (mainState.is(this)) {
				if (!player.isCreative()) {
					Block.dropResources(mainState, level, main, level.getBlockEntity(main), player, player.getMainHandItem());
				}
				level.setBlock(main, Blocks.AIR.defaultBlockState(), 35);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		return state.getValue(EXTENSION) ? List.of() : super.getDrops(state, builder);
	}
}
