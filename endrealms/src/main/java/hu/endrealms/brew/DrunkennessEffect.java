package hu.endrealms.brew;

import hu.endrealms.EndRealms;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Drunkenness, from brandy; one more brandy before it wears off raises it a level, up to IV. Every level walks slower and
 * stumbles more often and harder, now and then with a hiccup. From III the world goes dark for a few seconds every
 * twenty or so; at IV it does so more often, you also black out every twenty seconds or so, and sometimes drop what you hold.
 */
public class DrunkennessEffect extends MobEffect {
	DrunkennessEffect() {
		super(MobEffectCategory.HARMFUL, 0xC8862B);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, EndRealms.id("effect.drunkenness"), -0.06, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		RandomSource random = entity.getRandom();
		int strength = amplifier + 1;
		if (entity.onGround() && random.nextInt(220) < strength) {           // a stumble to one side
			double angle = random.nextDouble() * Math.PI * 2.0;
			double push = 0.18 + 0.1 * amplifier;
			entity.setDeltaMovement(entity.getDeltaMovement().add(new Vec3(Math.cos(angle) * push, 0.05, Math.sin(angle) * push)));
			if (entity instanceof ServerPlayer player) {
				player.connection.send(new ClientboundSetEntityMotionPacket(player));
			}
		}
		if (random.nextInt(500) < strength) {                                 // a hiccup
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, 1.7F + random.nextFloat() * 0.2F);
		}
		if (amplifier >= 2 && random.nextInt(amplifier >= 3 ? 300 : 400) == 0) {   // the world swims away for a moment
			later(level, entity, new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, false));
		}
		if (amplifier >= 3) {
			if (random.nextInt(400) == 0) {                                     // a short blackout
				later(level, entity, new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false, false));
			}
			if (entity instanceof ServerPlayer player && !player.getMainHandItem().isEmpty() && random.nextInt(1800) == 0) {
				player.drop(false);                                              // butterfingers
			}
		}
		return true;
	}

	/**
	 * Effects are ticked while the game walks its list of them, and an effect added to that list right then is lost; so
	 * the darkness and the blackout are added just after.
	 */
	private static void later(ServerLevel level, LivingEntity entity, MobEffectInstance effect) {
		level.getServer().execute(() -> {
			if (entity.isAlive()) {
				entity.addEffect(effect);
			}
		});
	}
}
