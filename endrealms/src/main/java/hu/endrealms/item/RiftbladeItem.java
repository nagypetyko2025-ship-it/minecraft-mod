package hu.endrealms.item;

import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Riftblade: a sword that, on use, carries its wielder up to 9 blocks along their line of sight, stopping at walls. */
public class RiftbladeItem extends Item {
	private static final double RANGE = 9.0;
	private static final double STEP = 0.5;
	private static final int COOLDOWN_TICKS = 8 * 20;

	public RiftbladeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!(user instanceof ServerPlayer player)) {
			return InteractionResult.SUCCESS;
		}
		ServerLevel here = player.level();
		Vec3 start = player.position();
		Vec3 step = player.getLookAngle().scale(STEP);
		Vec3 end = start;
		for (double travelled = STEP; travelled <= RANGE; travelled += STEP) {
			Vec3 next = end.add(step);
			if (!here.noCollision(player, player.getBoundingBox().move(next.subtract(start)))) {
				break;
			}
			end = next;
		}
		if (end.distanceToSqr(start) < 1.0) {
			return InteractionResult.FAIL;
		}
		here.sendParticles(ParticleTypes.PORTAL, start.x, start.y + 1.0, start.z, 30, 0.3, 0.7, 0.3, 0.2);
		player.teleportTo(here, end.x, end.y, end.z, Set.of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		here.sendParticles(ParticleTypes.REVERSE_PORTAL, end.x, end.y + 1.0, end.z, 30, 0.3, 0.7, 0.3, 0.05);
		here.playSound(null, end.x, end.y, end.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.6F);
		ItemStack stack = player.getItemInHand(hand);
		stack.hurtAndBreak(2, player, hand);
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}
}
