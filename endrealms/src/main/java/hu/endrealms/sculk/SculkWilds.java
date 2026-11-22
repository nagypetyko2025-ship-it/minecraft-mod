package hu.endrealms.sculk;

import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The plants and odd blocks of the Sculk dimension's stranger lands. */
public final class SculkWilds {
	private SculkWilds() {
	}

	/**
	 * Dangle Berry: a pale cluster hanging from the canopies of the Hanging Woods, which grow upside down. It needs
	 * something solid (or leaves) above it; breaking it gives the berries.
	 */
	public static class HangingBerry extends Block {
		private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 16, 12);

		public HangingBerry(Properties properties) {
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
		protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
			BlockState above = level.getBlockState(pos.above());
			return above.isFaceSturdy(level, pos.above(), Direction.DOWN) || above.is(net.minecraft.tags.BlockTags.LEAVES);
		}

		@Override
		protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
				BlockPos neighborPos, BlockState neighborState, RandomSource random) {
			return direction == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
					: super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		}
	}

	/**
	 * Thorn Vine: a black vine that climbs the Listening Pillars, a ladder of sorts. Now and then it carries glowing blue
	 * berries: use it to pick them, and they grow back in time.
	 */
	public static class ThornVine extends LadderBlock {
		public static final BooleanProperty BERRIES = BooleanProperty.create("berries");

		public ThornVine(Properties properties) {
			super(properties);
			registerDefaultState(defaultBlockState().setValue(BERRIES, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(BERRIES);
		}

		@Override
		protected boolean isRandomlyTicking(BlockState state) {
			return !state.getValue(BERRIES);
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			if (random.nextInt(12) == 0) {
				level.setBlock(pos, state.setValue(BERRIES, true), UPDATE_CLIENTS);
			}
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!state.getValue(BERRIES)) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				Block.popResource(level, pos, new ItemStack(ModItems.THORN_BERRY, 1 + level.getRandom().nextInt(2)));
				level.setBlock(pos, state.setValue(BERRIES, false), UPDATE_CLIENTS);
				level.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F);
			}
			return InteractionResult.SUCCESS;
		}
	}

	/** Ash Flower: grey heads at the foot of the pillars; brush through them and they burst into a cloud of ash. */
	public static class AshFlower extends SculkPlants.Plant {
		public AshFlower(Properties properties) {
			super(properties, Block.box(3, 0, 3, 13, 12, 13));
		}

		@Override
		protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
			if (level instanceof ServerLevel server && entity instanceof LivingEntity && server.getRandom().nextInt(6) == 0
					&& entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
				server.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 24, 0.4, 0.5, 0.4, 0.02);
				server.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.4, 0.5, 0.4, 0.01);
				server.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.5F, 0.6F);
			}
		}
	}

	/**
	 * The hidden heart of a great cracked bell in the Bell Valley. Every so often, when nobody is near it, the bell
	 * tolls: deep and far, and every Bellringer within reach of whoever hears it is drawn to where that player stands.
	 * If there are too few of them about, the toll calls one up out of the dark.
	 */
	public static class BellCore extends Block {
		private static final int QUIET_RADIUS = 18;
		private static final int HEARD_RADIUS = 110;

		public BellCore(Properties properties) {
			super(properties);
		}

		@Override
		protected boolean isRandomlyTicking(BlockState state) {
			return true;
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			if (random.nextInt(14) != 0) {
				return;
			}
			List<ServerPlayer> near = level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(QUIET_RADIUS), p -> !p.isSpectator());
			if (!near.isEmpty()) {
				return;                      // it only tolls when nobody stands by it
			}
			List<ServerPlayer> hearers = level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(HEARD_RADIUS), p -> !p.isSpectator() && !p.isCreative());
			if (hearers.isEmpty()) {
				return;
			}
			for (int i = 0; i < 3; i++) {
				level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 6.0F, 0.32F + i * 0.02F);
			}
			level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 4.0F, 0.4F);
			for (ServerPlayer player : hearers) {
				List<hu.endrealms.entity.Bellringer> ringers = level.getEntitiesOfClass(hu.endrealms.entity.Bellringer.class,
						player.getBoundingBox().inflate(64));
				for (hu.endrealms.entity.Bellringer ringer : ringers) {
					ringer.hearToll(player);
				}
				if (ringers.size() < 2) {
					hu.endrealms.entity.Bellringer.callUp(level, player, random);
				}
			}
		}
	}

}
