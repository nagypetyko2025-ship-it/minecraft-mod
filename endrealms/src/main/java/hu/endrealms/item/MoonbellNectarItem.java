package hu.endrealms.item;

import hu.endrealms.block.MoonbellBlock;
import hu.endrealms.registry.ModBlocks;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Moonbell Nectar: unlike milk, it only washes away harmful effects and leaves the helpful ones alone. */
public class MoonbellNectarItem extends Item {
	public MoonbellNectarItem(Properties properties) {
		super(properties);
	}

	/** On the top of End Soil it is planted instead of drunk: a Moonbell sprout that grows into a whole plant. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos soil = context.getClickedPos();
		BlockPos pos = soil.above();
		if (context.getClickedFace() != Direction.UP || !level.getBlockState(soil).is(ModBlocks.END_SOIL) || !level.getBlockState(pos).canBeReplaced()) {
			return InteractionResult.PASS;     // drunk as usual
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, MoonbellBlock.sprout(), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, pos);
			if (context.getPlayer() == null || !context.getPlayer().hasInfiniteMaterials()) {
				context.getItemInHand().shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (!level.isClientSide()) {
			for (MobEffectInstance effect : new ArrayList<>(entity.getActiveEffects())) {
				if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					entity.removeEffect(effect.getEffect());
				}
			}
		}
		return result;
	}
}
