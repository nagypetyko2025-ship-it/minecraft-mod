package hu.endrealms.block;

import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Moonbell: a three-block plant of the Astral Forest with glowing bells on top. Breaking any part removes all of it, and the
 * whole plant drops once: Bell Nectar by hand, the plant itself with shears. Nectar planted on End Soil starts a sprout
 * ({@link #AGE} 0 and 1, one block tall) that grows into a whole plant, so plants are farmed, not copied.
 */
public class MoonbellBlock extends VegetationBlock implements BonemealableBlock {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
	/** 0 and 1: a young plant, only the bottom block; 2: grown. Plants from before sprouts existed load as grown. */
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);
	private static final int TOP_PART = 2;
	private static final int GROWN = 2;
	private static final int GROW_CHANCE = 5;
	private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 16.0);
	private static final VoxelShape SPROUT_SHAPE = Block.column(6.0, 0.0, 10.0);

	public MoonbellBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(PART, 0).setValue(AGE, GROWN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART, AGE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return young(state) ? SPROUT_SHAPE : SHAPE;
	}

	private static boolean young(BlockState state) {
		return state.getValue(PART) == 0 && state.getValue(AGE) < GROWN;
	}

	/** A sprout, as Bell Nectar plants it. */
	public static BlockState sprout() {
		return ModBlocks.MOONBELL.defaultBlockState().setValue(AGE, 0);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(ModBlocks.END_SOIL);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		int part = state.getValue(PART);
		if (part == 0) {
			return super.canSurvive(state, level, pos);
		}
		BlockState below = level.getBlockState(pos.below());
		return below.is(this) && below.getValue(PART) == part - 1;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		int part = state.getValue(PART);
		boolean lostUpperPart = direction == Direction.UP && part < TOP_PART && !young(state)
				&& !(neighborState.is(this) && neighborState.getValue(PART) == part + 1);
		if (lostUpperPart || !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		boolean room = pos.getY() + TOP_PART <= level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)
				&& level.getBlockState(pos.above(TOP_PART)).canBeReplaced(context);
		return room ? this.defaultBlockState() : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		placeUpperParts(level, pos, Block.UPDATE_ALL);
	}

	private static void placeUpperParts(LevelAccessor level, BlockPos pos, int flags) {
		for (int part = 1; part <= TOP_PART; part++) {
			level.setBlock(pos.above(part), ModBlocks.MOONBELL.defaultBlockState().setValue(PART, part), flags);
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return young(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(GROW_CHANCE) == 0) {
			grow(level, pos, state);
		}
	}

	/** One step: sprout to bud, then bud to a whole plant if there is room above. */
	private static void grow(Level level, BlockPos pos, BlockState state) {
		int age = state.getValue(AGE);
		if (age + 1 < GROWN) {
			level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_ALL);
		} else if (roomAbove(level, pos)) {
			level.setBlock(pos, state.setValue(AGE, GROWN), Block.UPDATE_ALL);
			placeUpperParts(level, pos, Block.UPDATE_ALL);
		}
	}

	private static boolean roomAbove(LevelReader level, BlockPos pos) {
		return pos.getY() + TOP_PART <= level.getMaxY() && level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.above(TOP_PART)).isAir();
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return young(state) && (state.getValue(AGE) + 1 < GROWN || roomAbove(level, pos));
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		grow(level, pos, state);
	}

	/**
	 * Only the bottom block has loot. Whichever part the player breaks, the bottom one drops it (with the player's tool, so
	 * shears count) and the rest of the plant goes without dropping anything.
	 */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			BlockPos bottom = pos.below(state.getValue(PART));
			BlockState bottomState = level.getBlockState(bottom);
			if (state.getValue(PART) != 0 && bottomState.is(this) && !player.isCreative()) {
				Block.dropResources(bottomState, level, bottom, null, player, player.getMainHandItem());
			}
			for (int part = 0; part <= TOP_PART; part++) {
				BlockPos other = bottom.above(part);
				if (!other.equals(pos) && level.getBlockState(other).is(this)) {
					level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Puts down a whole plant during world generation. */
	public static void placeWhole(LevelAccessor level, BlockPos pos, int flags) {
		level.setBlock(pos, ModBlocks.MOONBELL.defaultBlockState(), flags);
		placeUpperParts(level, pos, flags);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(PART) == TOP_PART) {
			PrismBlocks.chime(level, pos, random, 10);
		}
	}
}
