package hu.endrealms.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Spire Nester: an eyeless, gaunt winged thing that nests on top of the Listening Pillars and circles silently over
 * its nest, wide and slow, rising and sinking. Harmless; it never lands and never comes after anyone.
 */
public class SpireNester extends PathfinderMob {
	private BlockPos nest;
	private float angle;
	private float radius;
	private float speed;

	public SpireNester(EntityType<? extends SpireNester> type, Level level) {
		super(type, level);
		setNoGravity(true);
		// its own random, not the level's: it may be made on a world generation thread
		this.radius = 6.0F + getRandom().nextFloat() * 6.0F;
		this.speed = (0.012F + getRandom().nextFloat() * 0.01F) * (getRandom().nextBoolean() ? 1 : -1);
		this.angle = getRandom().nextFloat() * Mth.TWO_PI;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.FLYING_SPEED, 0.4);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
		nest = blockPosition();
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}
		if (nest == null) {
			nest = blockPosition();
		}
		angle += speed;
		double height = 5.0 + Math.sin(tickCount * 0.01 + getId()) * 3.0;
		Vec3 goal = new Vec3(nest.getX() + 0.5 + Math.cos(angle) * radius, nest.getY() + height, nest.getZ() + 0.5 + Math.sin(angle) * radius);
		Vec3 step = goal.subtract(position()).scale(0.08);
		setDeltaMovement(step);
		setYRot((float) (Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90.0F);
		this.yBodyRot = getYRot();
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (nest != null) {
			output.putIntArray("Nest", new int[] {nest.getX(), nest.getY(), nest.getZ()});
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.getIntArray("Nest").ifPresent(n -> {
			if (n.length == 3) {
				nest = new BlockPos(n[0], n[1], n[2]);
			}
		});
	}

	@Override
	public int getAmbientSoundInterval() {
		return 600;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PHANTOM_FLAP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PHANTOM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PHANTOM_DEATH;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
