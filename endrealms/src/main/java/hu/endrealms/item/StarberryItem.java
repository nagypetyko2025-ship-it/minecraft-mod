package hu.endrealms.item;

import hu.endrealms.block.StarreedBlock;
import hu.endrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;

/** Starberry: eaten as usual, but on the top of End Soil it is planted as a Starreed shoot. */
public class StarberryItem extends Item {
	public StarberryItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos soil = context.getClickedPos();
		BlockPos pos = soil.above();
		if (context.getClickedFace() != Direction.UP || !level.getBlockState(soil).is(ModBlocks.END_SOIL) || !level.getBlockState(pos).canBeReplaced()) {
			return InteractionResult.PASS;     // eaten as usual
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, StarreedBlock.shoot(), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, pos);
			if (context.getPlayer() == null || !context.getPlayer().hasInfiniteMaterials()) {
				context.getItemInHand().shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
