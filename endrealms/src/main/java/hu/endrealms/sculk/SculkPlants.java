package hu.endrealms.sculk;

import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The plants of the Sculk dimension. They all grow on its ground: sculk, Sculk Soil, soul soil and the like. */
public final class SculkPlants {
	private SculkPlants() {
	}

	/** Whether a Sculk plant can stand on this block. */
	public static boolean ground(BlockState state) {
		return state.is(Blocks.SCULK) || state.is(ModBlocks.SCULK_SOIL) || state.is(Blocks.SOUL_SOIL) || state.is(BlockTags.DIRT)
				|| state.is(Blocks.PALE_MOSS_BLOCK) || state.is(ModBlocks.SCULK_STONE);
	}

	/** A small plant with no collision that needs Sculk ground under it. */
	public static class Plant extends Block {
		private final VoxelShape shape;

		public Plant(Properties properties, VoxelShape shape) {
			super(properties);
			this.shape = shape;
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return shape;
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return Shapes.empty();
		}

		@Override
		protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
			return ground(level.getBlockState(pos.below()));
		}

		@Override
		protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
				BlockPos neighborPos, BlockState neighborState, RandomSource random) {
			return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
					: super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		}
	}

	/** Which part of the three-block Watcher Grass a block is. */
	public enum Part implements StringRepresentable {
		BOTTOM("bottom"), MIDDLE("middle"), TOP("top");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	/** Where the eyes on top of the Watcher Grass look, or shut. */
	public enum Eyes implements StringRepresentable {
		NORTH("north"), EAST("east"), SOUTH("south"), WEST("west"), CLOSED("closed");

		private final String name;

		Eyes(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

		static Eyes toward(Direction direction) {
			return switch (direction) {
				case EAST -> EAST;
				case SOUTH -> SOUTH;
				case WEST -> WEST;
				default -> NORTH;
			};
		}
	}

	/**
	 * Watcher Grass: the common grass of the Sculk dimension, three blocks tall and growing thick. Its top carries a
	 * pair of faintly glowing eyes that turn after the nearest player ({@link SculkLife}) and shut when you sneak.
	 */
	public static class WatcherGrass extends Plant {
		public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
		public static final EnumProperty<Eyes> EYES = EnumProperty.create("eyes", Eyes.class);

		public WatcherGrass(Properties properties) {
			super(properties, Block.box(2, 0, 2, 14, 16, 14));
			registerDefaultState(stateDefinition.any().setValue(PART, Part.BOTTOM).setValue(EYES, Eyes.NORTH));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(PART, EYES);
		}

		@Override
		protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
			BlockState below = level.getBlockState(pos.below());
			return switch (state.getValue(PART)) {
				case BOTTOM -> ground(below);
				case MIDDLE -> below.is(this) && below.getValue(PART) == Part.BOTTOM;
				case TOP -> below.is(this) && below.getValue(PART) == Part.MIDDLE;
			};
		}

		@Override
		protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
				BlockPos neighborPos, BlockState neighborState, RandomSource random) {
			Part part = state.getValue(PART);
			boolean needsAbove = part != Part.TOP;
			if (direction == Direction.UP && needsAbove && !(neighborState.is(this) && neighborState.getValue(PART) == (part == Part.BOTTOM ? Part.MIDDLE : Part.TOP))) {
				return Blocks.AIR.defaultBlockState();
			}
			if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
				return Blocks.AIR.defaultBlockState();
			}
			return state;
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context) {
			BlockPos pos = context.getClickedPos();
			Level level = context.getLevel();
			if (pos.getY() + 2 < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)
					&& level.getBlockState(pos.above(2)).canBeReplaced(context)) {
				return defaultBlockState();
			}
			return null;
		}

		@Override
		public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
			level.setBlock(pos.above(), state.setValue(PART, Part.MIDDLE), UPDATE_ALL);
			level.setBlock(pos.above(2), state.setValue(PART, Part.TOP), UPDATE_ALL);
		}

		/** For world generation: the whole plant at once, if there is room. */
		public static boolean place(net.minecraft.world.level.LevelAccessor level, BlockPos pos, RandomSource random, int flags) {
			BlockState grass = ModBlocks.WATCHER_GRASS.defaultBlockState();
			if (!ground(level.getBlockState(pos.below())) || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above()) || !level.isEmptyBlock(pos.above(2))) {
				return false;
			}
			Eyes eyes = Eyes.values()[random.nextInt(4)];
			level.setBlock(pos, grass.setValue(PART, Part.BOTTOM), flags);
			level.setBlock(pos.above(), grass.setValue(PART, Part.MIDDLE), flags);
			level.setBlock(pos.above(2), grass.setValue(PART, Part.TOP).setValue(EYES, eyes), flags);
			return true;
		}

		/** Breaking any part from below in creative leaves no floating halves; the others fall through updateShape. */
		@Override
		public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
			if (!level.isClientSide() && player.isCreative() && state.getValue(PART) != Part.BOTTOM) {
				BlockPos bottom = pos.below(state.getValue(PART) == Part.TOP ? 2 : 1);
				if (level.getBlockState(bottom).is(this)) {
					level.setBlock(bottom, Blocks.AIR.defaultBlockState(), UPDATE_ALL | UPDATE_SUPPRESS_DROPS);
				}
			}
			return super.playerWillDestroy(level, pos, state, player);
		}
	}

	/** Turns the eyes of a Watcher Grass top toward a point, or shuts them. Returns whether anything changed. */
	static boolean look(ServerLevel level, BlockPos top, BlockState state, double x, double z, boolean shut) {
		Eyes eyes = shut ? Eyes.CLOSED : Eyes.toward(Direction.getApproximateNearest(x - (top.getX() + 0.5), 0, z - (top.getZ() + 0.5)));
		if (state.getValue(WatcherGrass.EYES) == eyes) {
			return false;
		}
		level.setBlock(top, state.setValue(WatcherGrass.EYES, eyes), Block.UPDATE_CLIENTS);
		return true;
	}
}
