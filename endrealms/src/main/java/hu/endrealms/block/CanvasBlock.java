package hu.endrealms.block;

import com.mojang.serialization.Codec;
import hu.endrealms.EndRealms;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A canvas, painted pixel by pixel: right-click its front with any dye to paint the pixel you look at, sneak with an
 * empty hand to scrape one off. Broken, it keeps its picture, like the carved pumpkin keeps its face.
 */
public class CanvasBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final DataComponentType<String> PAINT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EndRealms.id("paint"),
			DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
	private static final VoxelShape NORTH = Block.box(0, 0, 15, 16, 16, 16);
	private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 1);
	private static final VoxelShape EAST = Block.box(0, 0, 0, 1, 16, 16);
	private static final VoxelShape WEST = Block.box(15, 0, 0, 16, 16, 16);

	public CanvasBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	public static int widthOf(BlockState state) {
		return state.getBlock() instanceof LargeCanvasBlock ? 32 : 16;
	}

	/** The block that holds the paint: itself, or the left half of a large canvas. */
	public static BlockPos mainPos(BlockState state, BlockPos pos) {
		return LargeCanvasBlock.isExtension(state) ? pos.relative(right(state.getValue(FACING)).getOpposite()) : pos;
	}

	/** To a viewer standing in front of the canvas, this direction is to the right. */
	public static Direction right(Direction facing) {
		return facing.getCounterClockWise();
	}

	/** {u, v} of the pixel at a point on the front, counted from the left (main) block's bottom-left corner. */
	public static int[] pixelAt(BlockState state, BlockPos main, Vec3 hit) {
		Direction right = right(state.getValue(FACING));
		double along = right.getAxis() == Direction.Axis.X ? hit.x - main.getX() : hit.z - main.getZ();
		double u = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? along : 1.0 - along;
		return new int[] {Mth.clamp(Mth.floor(u * 16.0), 0, widthOf(state) - 1), Mth.clamp(Mth.floor((hit.y - main.getY()) * 16.0), 0, 15)};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> NORTH;
		};
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clicked = context.getClickedFace();
		Direction facing = clicked.getAxis().isHorizontal() ? clicked : context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CanvasBlockEntity(pos, state);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		ItemStack stack = new ItemStack(this);
		if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CanvasBlockEntity canvas && !canvas.isBlank()) {
			stack.set(PAINT, canvas.snapshot());
		}
		return List.of(stack);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		String paint = stack.get(PAINT);
		if (paint != null && level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas) {
			canvas.restore(paint);
		}
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CanvasBlock) || player.isSpectator() || hit.getDirection() != state.getValue(FACING)) {
				return InteractionResult.PASS;
			}
			ItemStack held = player.getItemInHand(hand);
			DyeColor dye = held.get(DataComponents.DYE);
			boolean scrape = held.isEmpty() && player.isSecondaryUseActive();
			if (dye == null && !scrape) {
				return InteractionResult.PASS;
			}
			BlockPos main = mainPos(state, pos);
			if (!level.isClientSide() && level.getBlockEntity(main) instanceof CanvasBlockEntity canvas) {
				int[] pixel = pixelAt(state, main, hit.getLocation());
				canvas.paint(pixel[0], pixel[1], scrape ? 0 : dye.getId() + 1);
				level.playSound(null, pos, scrape ? SoundEvents.WOOL_HIT : SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.35F, 1.6F);
			}
			return InteractionResult.SUCCESS;
		});
	}
}
