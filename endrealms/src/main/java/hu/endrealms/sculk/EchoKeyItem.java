package hu.endrealms.sculk;

import hu.endrealms.registry.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Echo Key: lights the great frame of reinforced deepslate in the heart of an Ancient City. Used on the frame (or on
 * the air just inside it), it fills the opening with Sculk Portal - but only if the opening is closed all round in one
 * upright plane, like a nether portal. Wears out after a few uses.
 */
public class EchoKeyItem extends Item {
	/** Larger than the Ancient City's opening; anything bigger is not a closed frame. */
	private static final int MAX_BLOCKS = 600;

	public EchoKeyItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos clicked = context.getClickedPos();
		if (!level.getBlockState(clicked).is(Blocks.REINFORCED_DEEPSLATE)) {
			return InteractionResult.PASS;
		}
		BlockPos start = clicked.relative(context.getClickedFace());
		List<BlockPos> opening = null;
		for (BlockPos from : new BlockPos[] {start, clicked.above()}) {
			for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
				opening = enclosed(level, from, axis);
				if (opening != null) {
					break;
				}
			}
			if (opening != null) {
				break;
			}
		}
		if (opening == null) {
			if (context.getPlayer() != null) {
				context.getPlayer().sendOverlayMessage(Component.translatable("item.endrealms.echo_key.no_frame"));
			}
			level.playSound(null, clicked, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0F, 0.6F);
			return InteractionResult.FAIL;
		}
		BlockState portal = ModBlocks.SCULK_PORTAL.defaultBlockState().setValue(SculkPortalBlock.POOL, false);
		for (BlockPos pos : opening) {
			level.setBlock(pos, portal, 2);
			if (level.getRandom().nextInt(4) == 0) {
				level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
			}
		}
		level.playSound(null, clicked, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 2.0F, 0.6F);
		level.playSound(null, clicked, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 2.0F, 0.5F);
		if (context.getPlayer() != null) {
			context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
					? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * The open blocks connected to {@code from} in the upright plane along {@code axis}, if they are closed in on every
	 * side within that plane; null if they run off (no frame there) or there is nothing to fill.
	 */
	private static List<BlockPos> enclosed(ServerLevel level, BlockPos from, Direction.Axis axis) {
		if (!open(level, from)) {
			return null;
		}
		Direction[] sides = axis == Direction.Axis.X
				? new Direction[] {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}
				: new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		List<BlockPos> found = new ArrayList<>();
		queue.add(from);
		seen.add(from);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			found.add(pos);
			if (found.size() > MAX_BLOCKS) {
				return null;
			}
			for (Direction side : sides) {
				BlockPos next = pos.relative(side);
				if (!seen.contains(next) && open(level, next)) {
					seen.add(next);
					queue.add(next);
				}
			}
		}
		return found;
	}

	/**
	 * Air, or something thin that the portal takes the place of: sculk veins (the Ancient City's frame is full of them),
	 * moss, snow, cobwebs - anything you could walk through. Not the frame itself, and not water or lava.
	 */
	private static boolean open(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.is(Blocks.SCULK_VEIN)) {
			return true;
		}
		return state.getFluidState().isEmpty() && !state.is(ModBlocks.SCULK_PORTAL)
				&& (state.canBeReplaced() || state.getCollisionShape(level, pos).isEmpty());
	}
}
