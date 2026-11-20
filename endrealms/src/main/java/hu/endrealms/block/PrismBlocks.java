package hu.endrealms.block;

import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Prism Wastes crystals: amethyst-like blocks that chime softly while a player walks past them. */
public final class PrismBlocks {
	private static final double CHIME_RANGE = 5.0;

	private PrismBlocks() {
	}

	/** Client side only (called from animateTick); oneIn thins out the sound so a whole spire is not deafening. */
	static void chime(Level level, BlockPos pos, RandomSource random, int oneIn) {
		if (random.nextInt(oneIn) != 0) {
			return;
		}
		Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, CHIME_RANGE, false);
		if (player != null && player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AMETHYST_BLOCK_CHIME,
					SoundSource.BLOCKS, 0.35F, 0.7F + random.nextFloat() * 0.8F, false);
		}
	}

	public static class Crystal extends AmethystBlock {
		public Crystal(Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			chime(level, pos, random, 90);
		}
	}

	public static class Cluster extends AmethystClusterBlock {
		public Cluster(float height, float width, Properties properties) {
			super(height, width, properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			chime(level, pos, random, 12);
		}
	}

	/** Grows small bud -> medium -> large -> cluster on its free sides, exactly as slowly as budding amethyst. */
	public static class Budding extends Crystal {
		public Budding(Properties properties) {
			super(properties);
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			if (random.nextInt(BuddingAmethystBlock.GROWTH_CHANCE) != 0) {
				return;
			}
			Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
			BlockPos target = pos.relative(direction);
			BlockState current = level.getBlockState(target);
			Block next = null;
			if (BuddingAmethystBlock.canClusterGrowAtState(current)) {
				next = ModBlocks.SMALL_PRISM_BUD;
			} else if (current.is(ModBlocks.SMALL_PRISM_BUD) && current.getValue(AmethystClusterBlock.FACING) == direction) {
				next = ModBlocks.MEDIUM_PRISM_BUD;
			} else if (current.is(ModBlocks.MEDIUM_PRISM_BUD) && current.getValue(AmethystClusterBlock.FACING) == direction) {
				next = ModBlocks.LARGE_PRISM_BUD;
			} else if (current.is(ModBlocks.LARGE_PRISM_BUD) && current.getValue(AmethystClusterBlock.FACING) == direction) {
				next = ModBlocks.PRISM_CLUSTER;
			}
			if (next != null) {
				level.setBlockAndUpdate(target, next.defaultBlockState()
						.setValue(AmethystClusterBlock.FACING, direction)
						.setValue(AmethystClusterBlock.WATERLOGGED, current.getFluidState().getType() == Fluids.WATER));
			}
		}
	}
}
