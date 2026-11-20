package hu.endrealms.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Ash Wraith: the hunter of the Scorched Barrens. It spits embers from a distance, sets what it strikes on fire and
 * slows it, and when it falls it leaves a choking cloud of ash behind.
 */
public class AshWraith extends Monster {
	private static final double MIN_SPIT_RANGE = 5.0;
	private static final double MAX_SPIT_RANGE = 18.0;

	private int spitCooldown = 15;

	public AshWraith(EntityType<? extends AshWraith> type, Level level) {
		super(type, level);
		this.xpReward = 14;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 32.0)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.29)
				.add(Attributes.FOLLOW_RANGE, 26.0);
	}

	public static boolean checkAshWraithSpawnRules(EntityType<AshWraith> type, ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random); // embers light the whole biome
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (spitCooldown > 0) {
			spitCooldown--;
		}
		LivingEntity target = getTarget();
		if (target == null || spitCooldown > 0) {
			return;
		}
		double distance = distanceTo(target);
		if (distance < MIN_SPIT_RANGE || distance > MAX_SPIT_RANGE || !getSensing().hasLineOfSight(target)) {
			return;
		}
		spitCooldown = 60 + random.nextInt(40);
		Vec3 eye = getEyePosition();
		Vec3 direction = target.getEyePosition().subtract(eye).normalize();
		SmallFireball ember = new SmallFireball(level, this, direction);
		ember.setPos(eye.x + direction.x, eye.y - 0.2, eye.z + direction.z);
		level.addFreshEntity(ember);
		playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.7F);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.igniteForSeconds(4.0F);
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0), this);
		}
		return hit;
	}

	@Override
	public void die(DamageSource source) {
		boolean first = !this.dead;
		super.die(source);
		if (first && level() instanceof ServerLevel level) {
			AreaEffectCloud cloud = new AreaEffectCloud(level, getX(), getY(), getZ());
			cloud.setCustomParticle(ParticleTypes.LARGE_SMOKE);
			cloud.setRadius(3.0F);
			cloud.setRadiusPerTick(-0.03F);
			cloud.setDuration(70);
			cloud.setWaitTime(0);
			cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
			cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
			level.addFreshEntity(cloud);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide() && random.nextInt(3) == 0) {
			level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLAZE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLAZE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLAZE_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}
}
