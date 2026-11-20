package hu.endrealms.item;

import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModBlocks;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Phase Pearl: right-clicking a Phase Anchor binds the pearl to it, using the pearl anywhere else jumps to the top of
 * that anchor, even from another dimension. Each jump costs one charge and starts a 30 second cooldown; an anvil and
 * phasite shards recharge it. It refuses to jump when the anchor is gone or covered, and an empty pearl stays intact.
 */
public class PhasePearlItem extends Item {
	public static final DataComponentType<GlobalPos> TARGET = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EndRealms.id("phase_target"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
	private static final int COOLDOWN_TICKS = 30 * 20;

	public PhasePearlItem(Properties properties) {
		super(properties);
	}

	private static boolean clear(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	private static boolean canStand(ServerLevel level, BlockPos pos) {
		return clear(level, pos) && clear(level, pos.above()) && !clear(level, pos.below());
	}

	private static void tell(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable("item.endrealms.phase_pearl." + key), true);
	}

	private static void effects(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 40, 0.4, 0.8, 0.4, 0.2);
		level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockPos clicked = context.getClickedPos();
		if (!context.getLevel().getBlockState(clicked).is(ModBlocks.PHASE_ANCHOR)) {
			return InteractionResult.PASS;      // falls through to use(), so aiming at a block still jumps
		}
		if (context.getPlayer() instanceof ServerPlayer player) {
			ServerLevel here = player.level();
			context.getItemInHand().set(TARGET, GlobalPos.of(here.dimension(), clicked));
			here.playSound(null, clicked, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.4F);
			tell(player, "marked");
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!(user instanceof ServerPlayer player)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = player.getItemInHand(hand);
		ServerLevel here = player.level();
		GlobalPos target = stack.get(TARGET);
		if (target == null) {
			tell(player, "no_target");
			return InteractionResult.FAIL;
		}
		if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
			tell(player, "empty");
			return InteractionResult.FAIL;
		}
		ServerLevel destination = here.getServer().getLevel(target.dimension());
		if (destination == null || !destination.getBlockState(target.pos()).is(ModBlocks.PHASE_ANCHOR)) {
			tell(player, "anchor_missing");
			return InteractionResult.FAIL;
		}
		BlockPos landing = target.pos().above();
		if (!canStand(destination, landing)) {
			tell(player, "blocked");
			return InteractionResult.FAIL;
		}
		effects(here, player.getX(), player.getY(), player.getZ());
		double x = landing.getX() + 0.5;
		double y = landing.getY();
		double z = landing.getZ() + 0.5;
		player.teleportTo(destination, x, y, z, Set.of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		effects(destination, x, y, z);
		stack.hurtAndBreak(1, player, hand);
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(TARGET) || super.isFoil(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		GlobalPos target = stack.get(TARGET);
		if (target == null) {
			tooltip.accept(Component.translatable("item.endrealms.phase_pearl.unmarked").withStyle(ChatFormatting.GRAY));
		} else {
			BlockPos pos = target.pos();
			tooltip.accept(Component.translatable("item.endrealms.phase_pearl.target", pos.getX(), pos.getY(), pos.getZ(),
					Component.translatable("item.endrealms.phase_pearl.dimension." + target.dimension().identifier().getPath())).withStyle(ChatFormatting.AQUA));
		}
		tooltip.accept(Component.translatable("item.endrealms.phase_pearl.charges", stack.getMaxDamage() - 1 - stack.getDamageValue(), stack.getMaxDamage() - 1).withStyle(ChatFormatting.GRAY));
	}
}
