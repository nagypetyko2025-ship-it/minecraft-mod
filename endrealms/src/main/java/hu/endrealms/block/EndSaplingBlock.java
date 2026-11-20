package hu.endrealms.block;

import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/** A sapling that can be planted on end stone, end soil (or dirt), but only grows - naturally or with bone meal - in the End. */
public class EndSaplingBlock extends SaplingBlock {
	public EndSaplingBlock(TreeGrower treeGrower, Properties properties) {
		super(treeGrower, properties);
	}

	private static boolean inTheEnd(LevelReader level) {
		return level instanceof Level l && l.dimension() == Level.END;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(Blocks.END_STONE) || state.is(ModBlocks.END_SOIL) || super.mayPlaceOn(state, level, pos);
	}

	/** The End has no daylight, so unlike vanilla saplings this one grows there without needing light. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (inTheEnd(level) && random.nextInt(TICK_CHANCE_FOR_SAPLING_GROWTH) == 0) {
			advanceTree(level, pos, state, random);
		}
	}

	@Override
	public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		if (inTheEnd(level)) {
			super.advanceTree(level, pos, state, random);
		}
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return inTheEnd(level) && super.isValidBonemealTarget(level, pos, state, source);
	}
}
