package hu.endrealms.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Deep Lurker: the eel-like predator of the Astral Basin's lake. It only hunts what is in the water, swims nearly as
 * fast as a sprinting swimmer, and each bite slows its prey and steals its breath. On land it gasps and dries out.
 */
public class DeepLurker extends Monster {
	private static final double HUNT_SPEED = 0.24;
	private static final double IDLE_SPEED = 0.07;
	private static final int STOLEN_AIR = 30;
	private static final double BITE_RANGE_SQR = 6.0;
	private static final double HOLD_OFF_SQR = 3.2;   // stops short of its prey instead of shoving it through the lake

	private int biteCooldown;
	private int dryTicks;
	private int wanderTicks;
	private Vec3 wanderTo;

	public DeepLurker(EntityType<? extends DeepLurker> type, Level level) {
		super(type, level);
		this.xpReward = 20;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 44.0)
				.add(Attributes.ATTACK_DAMAGE, 7.0)
				.add(Attributes.ARMOR, 6.0)
				.add(Attributes.FOLLOW_RANGE, 26.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	@Override
	protected void registerGoals() {
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (living, level) -> living.isInWater()));
	}

	private boolean water(BlockPos pos) {
		return !level().getFluidState(pos).isEmpty();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = getTarget();
		if (target != null && !target.isInWater()) {
			setTarget(null);
			target = null;
		}
		boolean swimming = isInWater();
		setNoGravity(swimming);
		if (!swimming) {
			if (++dryTicks % 20 == 0) {
				hurtServer(level, damageSources().dryOut(), 2.0F);
			}
			return;
		}
		dryTicks = 0;
		Vec3 goal = target != null ? target.getBoundingBox().getCenter() : wanderGoal();
		Vec3 offset = goal.subtract(position());
		if (target != null && offset.lengthSqr() < HOLD_OFF_SQR) {
			setDeltaMovement(Vec3.ZERO);
			faceTowards(offset.normalize());
		} else if (offset.lengthSqr() > 0.05) {
			Vec3 direction = offset.normalize();
			if (direction.y > 0.0 && !water(BlockPos.containing(getX(), getY() + getBbHeight() + 0.4, getZ()))) {
				direction = new Vec3(direction.x, -0.15, direction.z).normalize();   // stays under the surface
			}
			setDeltaMovement(direction.scale(target != null ? HUNT_SPEED : IDLE_SPEED));
			faceTowards(direction);
		}
		if (biteCooldown > 0) {
			biteCooldown--;
		}
		if (target != null && biteCooldown <= 0 && distanceToSqr(target) < BITE_RANGE_SQR) {
			biteCooldown = 40;
			doHurtTarget(level, target);
		}
	}

	private void faceTowards(Vec3 direction) {
		float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
		setXRot((float) (-Math.asin(Mth.clamp(direction.y, -1.0, 1.0)) * Mth.RAD_TO_DEG));
	}

	private Vec3 wanderGoal() {
		if (wanderTo == null || --wanderTicks <= 0 || position().distanceToSqr(wanderTo) < 2.0) {
			wanderTicks = 60 + random.nextInt(80);
			Vec3 candidate = position().add(random.nextDouble() * 16.0 - 8.0, random.nextDouble() * 6.0 - 3.0, random.nextDouble() * 16.0 - 8.0);
			wanderTo = water(BlockPos.containing(candidate)) ? candidate : position();
		}
		return wanderTo;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.setAirSupply(Math.max(0, living.getAirSupply() - STOLEN_AIR));
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1), this);
		}
		return hit;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isInWater() ? SoundEvents.GUARDIAN_AMBIENT : SoundEvents.GUARDIAN_AMBIENT_LAND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GUARDIAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GUARDIAN_DEATH;
	}
}
