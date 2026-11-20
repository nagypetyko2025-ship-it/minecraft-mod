package hu.endrealms.brew;

import hu.endrealms.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Fermentation Station: a copper-banded cask; while it works, its window glows amber and it bubbles now and then. */
public class FermentationStationBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty FERMENTING = BooleanProperty.create("fermenting");

	public FermentationStationBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FERMENTING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FERMENTING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FermentationStationBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FERMENTATION_STATION, FermentationStationBlockEntity::serverTick);
	}

	/** A water bucket pours one bucket in (up to ten); an empty bucket takes one back out. Anything else opens the window. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		boolean filling = stack.is(Items.WATER_BUCKET);
		if (!filling && !stack.is(Items.BUCKET) || !(level.getBlockEntity(pos) instanceof FermentationStationBlockEntity station)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (filling ? station.water() >= FermentationStationBlockEntity.MAX_WATER : station.water() <= 0) {
			return filling ? InteractionResult.FAIL : InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			if (filling) {
				station.addWater();
			} else {
				station.takeWater();
			}
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(filling ? Items.BUCKET : Items.WATER_BUCKET)));
			level.playSound(null, pos, filling ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, filling ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FermentationStationBlockEntity station) {
			player.openMenu(station);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(FERMENTING)) {
			return;
		}
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 1.02,
					pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
		}
		if (random.nextInt(60) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.3F, 0.6F, false);
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}
}
