package hu.endrealms.block;

import com.mojang.serialization.Codec;
import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModBlocks;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * A pumpkin carved pixel by pixel. Shears on a plain pumpkin, a vanilla carved pumpkin or a jack o'lantern turn it into
 * this block; each use cuts the pixel the player is looking at, and sneaking puts one back. A torch lights it from
 * inside, and then the cut pixels glow. Broken, it keeps its design and light. Pixels are numbered by two world axes
 * of the face: see {@link #axes}.
 */
public class CarvedPumpkinBlock extends Block implements EntityBlock {
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	/** The pixel words of the block entity, plus one final word: 1 if the pumpkin is lit. */
	public static final DataComponentType<List<Long>> CARVING = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EndRealms.id("carving"),
			DataComponentType.<List<Long>>builder().persistent(Codec.LONG.listOf()).networkSynchronized(ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list())).build());
	private static final int SEEDS = 4;
	/** The face of a vanilla carved pumpkin as bit rows, top row first, bit 0 at the left as seen from the front. */
	private static final int[] VANILLA_FACE = {0x0000, 0x0000, 0x0430, 0x0E70, 0x1E78, 0x1E78, 0x3E78, 0x3C30, 0x0000, 0x2928, 0x3FFC, 0x7FFC, 0x6ED8, 0x0848, 0x0000, 0x0000};

	public CarvedPumpkinBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CarvedPumpkinBlockEntity(pos, state);
	}

	/** Placed on snow, iron or copper like a vanilla pumpkin, it builds the golem (see GolemPumpkinMixin). */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(state.getBlock())) {
			((hu.endrealms.mixin.GolemBuilderInvoker) Blocks.CARVED_PUMPKIN).endrealms$trySpawnGolem(level, pos);
		}
	}

	/** The two world axes a face's pixels run along: {0 = x, 1 = y, 2 = z}. */
	public static int[] axes(Direction face) {
		return switch (face.getAxis()) {
			case X -> new int[] {2, 1};
			case Y -> new int[] {0, 2};
			case Z -> new int[] {0, 1};
		};
	}

	private static int pixel(double fraction) {
		return Mth.clamp(Mth.floor(fraction * 16.0), 0, 15);
	}

	/** {a, b} of the pixel at a point on the block's surface. */
	public static int[] pixelAt(BlockPos pos, Direction face, Vec3 hit) {
		double[] local = {hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ()};
		int[] axes = axes(face);
		return new int[] {pixel(local[axes[0]]), pixel(local[axes[1]])};
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		ItemStack stack = new ItemStack(this);
		if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CarvedPumpkinBlockEntity pumpkin) {
			stack.set(CARVING, pumpkin.snapshot(state.getValue(LIT)));
		}
		return List.of(stack);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		List<Long> carving = stack.get(CARVING);
		if (carving != null && level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin) {
			pumpkin.restore(carving);
			if (carving.size() > CarvedPumpkinBlockEntity.LONGS && carving.get(CarvedPumpkinBlockEntity.LONGS) != 0L) {
				level.setBlock(pos, state.setValue(LIT, true), 3);
			}
		}
	}

	private static boolean carvable(BlockState state) {
		return state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN) || state.is(ModBlocks.CARVED_PUMPKIN);
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack held = player.getItemInHand(hand);
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (player.isSpectator()) {
				return InteractionResult.PASS;
			}
			if (held.is(Items.TORCH) && state.is(ModBlocks.CARVED_PUMPKIN) && !state.getValue(LIT)) {
				if (!level.isClientSide()) {
					level.setBlock(pos, state.setValue(LIT, true), 3);
					level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
					held.consume(1, player);
				}
				return InteractionResult.SUCCESS;
			}
			if (!held.is(Items.SHEARS) || !carvable(state)) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				carve(player, level, pos, state, hit.getDirection(), hit.getLocation(), held, hand);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** Copies the printed face of a vanilla carved pumpkin into pixels, so carving starts from what was already there. */
	private static void stampVanillaFace(CarvedPumpkinBlockEntity pumpkin, Direction face) {
		boolean mirrored = face == Direction.NORTH || face == Direction.EAST;
		for (int row = 0; row < 16; row++) {
			for (int column = 0; column < 16; column++) {
				if ((VANILLA_FACE[row] >> column & 1) != 0) {
					pumpkin.setCarved(face, mirrored ? 15 - column : column, 15 - row, true);
				}
			}
		}
	}

	private static void carve(Player player, Level level, BlockPos pos, BlockState state, Direction face, Vec3 hit, ItemStack shears, InteractionHand hand) {
		if (!state.is(ModBlocks.CARVED_PUMPKIN)) {
			boolean vanillaFace = !state.is(Blocks.PUMPKIN);
			Direction printed = vanillaFace ? state.getValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING) : null;
			level.setBlock(pos, ModBlocks.CARVED_PUMPKIN.defaultBlockState().setValue(LIT, state.is(Blocks.JACK_O_LANTERN)), 3);
			if (vanillaFace) {
				if (level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin) {
					stampVanillaFace(pumpkin, printed);
				}
			} else {
				popResource(level, pos.relative(face), new ItemStack(Items.PUMPKIN_SEEDS, SEEDS));
			}
		}
		if (!(level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin)) {
			return;
		}
		int[] pixel = pixelAt(pos, face, hit);
		boolean carve = !player.isShiftKeyDown();
		if (pumpkin.isCarved(face, pixel[0], pixel[1]) == carve) {
			return;
		}
		pumpkin.setCarved(face, pixel[0], pixel[1], carve);
		level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 0.35F, 1.2F + level.getRandom().nextFloat() * 0.4F);
		if (carve && level.getRandom().nextInt(8) == 0) {
			shears.hurtAndBreak(1, player, hand);
		}
	}
}
