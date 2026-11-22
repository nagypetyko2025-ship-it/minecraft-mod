package hu.endrealms.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Dangler: a long, pale, thin thing that hangs head down from the canopies of the Hanging Woods, so still you take it
 * for a branch. When someone walks underneath, it lets go and drops on them. Once down it hunts like any monster.
 * Strong enough for Chorusite gear.
 */
public class Dangler extends Monster {
	private static final EntityDataAccessor<Boolean> HANGING = SynchedEntityData.defineId(Dangler.class, EntityDataSerializers.BOOLEAN);
	/** How far to the side and how far below it notices someone. */
	private static final double DROP_SIDE = 2.5;
	private static final double DROP_BELOW = 10.0;

	public Dangler(EntityType<? extends Dangler> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 50.0)
				.add(Attributes.ATTACK_DAMAGE, 11.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.ARMOR, 6.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(HANGING, false);
	}

	public boolean isHanging() {
		return entityData.get(HANGING);
	}

	private double hangX = Double.NaN;
	private double hangY;
	private double hangZ;

	private void setHanging(boolean hanging) {
		hangX = Double.NaN;
		entityData.set(HANGING, hanging);
		setNoGravity(hanging);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** When it comes into the world it looks up for a branch to hang from. */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
		hangFromAbove(level);
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	/** Looks up to 14 blocks above for something to hang from, and hangs there if it finds it. */
	public void hangFromAbove(net.minecraft.world.level.LevelReader level) {
		BlockPos.MutableBlockPos pos = blockPosition().mutable();
		for (int up = 0; up <= 14; up++) {
			pos.setY(getBlockY() + up);
			if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.DOWN) || level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) {
				setPos(getX(), pos.getY() - getBbHeight(), getZ());
				setHanging(true);
				return;
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() || !isHanging()) {
			return;
		}
		setDeltaMovement(Vec3.ZERO);              // hanging, it does not move at all, whatever its goals want
		getNavigation().stop();
		if (hangX == hangX) {
			setPos(hangX, hangY, hangZ);
		} else {
			hangX = getX();
			hangY = getY();
			hangZ = getZ();
		}
		BlockPos above = BlockPos.containing(getX(), getBoundingBox().maxY + 0.1, getZ());
		boolean holding = level().getBlockState(above).isFaceSturdy(level(), above, Direction.DOWN) || level().getBlockState(above).is(net.minecraft.tags.BlockTags.LEAVES);
		Player below = level().getNearestPlayer(getX(), getY() - DROP_BELOW / 2, getZ(), DROP_BELOW, p -> !((Player) p).isCreative() && !p.isSpectator()
				&& p.getY() < getY() && Math.abs(p.getX() - getX()) < DROP_SIDE && Math.abs(p.getZ() - getZ()) < DROP_SIDE);
		if (!holding || below != null) {
			drop(below);
		}
	}

	private void drop(Player target) {
		setHanging(false);
		playSound(SoundEvents.PHANTOM_SWOOP, 1.4F, 0.5F);
		if (target != null) {
			setTarget(target);
		}
	}

	/** Hit while hanging, it lets go at once. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isHanging()) {
			drop(source.getEntity() instanceof Player player ? player : null);
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean isPushable() {
		return !isHanging() && super.isPushable();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Hanging", isHanging());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setHanging(input.getBooleanOr("Hanging", false));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isHanging() ? null : SoundEvents.PHANTOM_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.45F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PHANTOM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PHANTOM_DEATH;
	}
}
