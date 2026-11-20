package hu.endrealms.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Ash Meal: a stronger bone meal, one piece applies several rounds of growth and none of them can fail. */
public class AshMealItem extends Item {
	private static final int ROUNDS = 6;
	private static final int GROWTH_PARTICLES = 1505;

	public AshMealItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof BonemealableBlock plant) || !plant.isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION)) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			for (int round = 0; round < ROUNDS; round++) {
				BlockState current = level.getBlockState(pos);
				if (!(current.getBlock() instanceof BonemealableBlock growing) || !growing.isValidBonemealTarget(level, pos, current, BonemealSource.INTERACTION)) {
					break;
				}
				growing.performBonemeal(server, server.getRandom(), pos, current, BonemealSource.INTERACTION);
			}
			level.levelEvent(GROWTH_PARTICLES, pos, 15);
			context.getItemInHand().consume(1, context.getPlayer());
		}
		return InteractionResult.SUCCESS;
	}
}
