package hu.endrealms.brew;

import hu.endrealms.EndRealms;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Passing out: reaching Drunkenness IV turns the screen fully black and the drinker cannot move for a few seconds. Then
 * they come to somewhere else within {@link #RANGE} blocks, with their inventory jumbled up and every effect gone,
 * the Drunkenness too. The black screen itself is drawn by the client while it sees the {@link #EFFECT} on its player.
 */
public final class Blackout {
	public static final int TICKS = 20 * 4;
	public static final int RANGE = 100;

	/** Hidden effect that holds the drinker in place and tells their client to draw the black screen. */
	public static final class Effect extends MobEffect {
		Effect() {
			super(MobEffectCategory.HARMFUL, 0x000000);
			addAttributeModifier(Attributes.MOVEMENT_SPEED, EndRealms.id("effect.blackout"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
			addAttributeModifier(Attributes.JUMP_STRENGTH, EndRealms.id("effect.blackout"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
	}

	public static final net.minecraft.core.Holder<MobEffect> EFFECT = net.minecraft.core.Registry.registerForHolder(
			net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT, EndRealms.id("blackout"), new Effect());

	/** Players who are out, and how many ticks until they come to. */
	private static final Map<UUID, Integer> OUT = new HashMap<>();

	private Blackout() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Blackout::tick);
	}

	public static void start(ServerPlayer player) {
		if (OUT.containsKey(player.getUUID())) {
			return;
		}
		OUT.put(player.getUUID(), TICKS);
		player.addEffect(new MobEffectInstance(EFFECT, TICKS + 10, 0, false, false, false));
		player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
	}

	private static void tick(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Integer>> it = OUT.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			int left = entry.getValue() - 1;
			if (left > 0) {
				entry.setValue(left);
				continue;
			}
			it.remove();
			wakeUp(player);
		}
	}

	private static void wakeUp(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos spot = findSpot(level, player.blockPosition(), player.getRandom());
		if (spot != null) {
			player.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), player.getRandom().nextFloat() * 360.0F, 0.0F, true);
		}
		player.resetFallDistance();
		shuffle(player.getInventory(), new Random(player.getRandom().nextLong()));
		player.removeAllEffects();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8F, 0.8F);
		player.sendOverlayMessage(Component.translatable("drunk.endrealms.woke"));
	}

	/** Jumbles the hotbar and the main inventory; armor and the offhand stay where they are. */
	static void shuffle(Inventory inventory, Random random) {
		int size = inventory.getNonEquipmentItems().size();
		List<ItemStack> items = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			items.add(inventory.getItem(i));
		}
		Collections.shuffle(items, random);
		for (int i = 0; i < size; i++) {
			inventory.setItem(i, items.get(i));
		}
		inventory.setChanged();
	}

	/** A safe place to stand somewhere within range: solid ground, room for the body, no liquid or fire. */
	static BlockPos findSpot(ServerLevel level, BlockPos center, RandomSource random) {
		boolean ceiling = level.dimensionType().hasCeiling();
		for (int attempt = 0; attempt < 64; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 10.0 + random.nextDouble() * (RANGE - 10.0);
			int x = center.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = center.getZ() + (int) Math.round(Math.sin(angle) * distance);
			if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, center.getY(), z))) {
				continue;
			}
			int top = ceiling ? level.getMinY() + level.getLogicalHeight() - 2 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			int lowest = Math.max(level.getMinY() + 1, top - (ceiling ? 128 : 4));
			for (int y = top; y >= lowest; y--) {
				BlockPos feet = new BlockPos(x, y, z);
				if (standable(level, feet)) {
					return feet;
				}
			}
		}
		return null;
	}

	private static boolean standable(ServerLevel level, BlockPos feet) {
		BlockState below = level.getBlockState(feet.below());
		BlockState body = level.getBlockState(feet);
		BlockState head = level.getBlockState(feet.above());
		return !below.getCollisionShape(level, feet.below()).isEmpty() && below.getFluidState().isEmpty()
				&& !below.is(Blocks.MAGMA_BLOCK) && !below.is(Blocks.CACTUS) && !below.is(BlockTags.FIRE) && !below.is(Blocks.POWDER_SNOW)
				&& body.getCollisionShape(level, feet).isEmpty() && body.getFluidState().isEmpty() && !body.is(BlockTags.FIRE)
				&& head.getCollisionShape(level, feet.above()).isEmpty() && head.getFluidState().isEmpty();
	}
}
