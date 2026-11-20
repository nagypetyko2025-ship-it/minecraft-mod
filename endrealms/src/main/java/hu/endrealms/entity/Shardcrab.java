package hu.endrealms.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shardcrab: an ambush predator of the Prism Wastes. While nothing is near it sits motionless with its legs drawn
 * in and looks like a crystal outcrop; a player within {@link #AMBUSH_RANGE} blocks wakes it, and it then chases
 * up to its follow range. After a few calm seconds it settles down and disguises itself again wherever it is.
 */
public class Shardcrab extends Monster {
	private static final EntityDataAccessor<Boolean> DISGUISED =
			SynchedEntityData.defineId(Shardcrab.class, EntityDataSerializers.BOOLEAN);
	private static final double AMBUSH_RANGE = 5.0;
	private static final int CALM_TICKS_TO_DISGUISE = 60;

	private int calmTicks;
	/** Client only: 0 = fully disguised, 1 = standing up; eased so the reveal is an animation, not a pop. */
	private float emerge;
	private float previousEmerge;

	public Shardcrab(EntityType<? extends Shardcrab> type, Level level) {
		super(type, level);
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.ATTACK_DAMAGE, 7.0)
				.add(Attributes.ARMOR, 12.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 18.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	public static boolean checkShardcrabSpawnRules(EntityType<Shardcrab> type, ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random); // the crystals light the biome
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DISGUISED, true);
	}

	public boolean isDisguised() {
		return this.entityData.get(DISGUISED);
	}

	public float getEmerge(float partialTick) {
		return this.previousEmerge + (this.emerge - this.previousEmerge) * partialTick;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
		// no stroll goal on purpose: a crystal that walks around is not much of a disguise
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
			@Override
			protected double getFollowDistance() {
				return Shardcrab.this.getTarget() == null ? AMBUSH_RANGE : super.getFollowDistance();
			}
		});
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.getTarget() != null) {
			this.calmTicks = 0;
			if (this.isDisguised()) {
				this.entityData.set(DISGUISED, false);
				this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.7F);
			}
		} else if (!this.isDisguised() && this.onGround() && ++this.calmTicks >= CALM_TICKS_TO_DISGUISE) {
			this.entityData.set(DISGUISED, true);
			this.getNavigation().stop();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.previousEmerge = this.emerge;
			float goal = this.isDisguised() ? 0.0F : 1.0F;
			this.emerge += Math.signum(goal - this.emerge) * Math.min(0.15F, Math.abs(goal - this.emerge));
		}
	}

	@Override
	public boolean isPushable() {
		return !this.isDisguised() && super.isPushable();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.isDisguised() ? null : SoundEvents.AMETHYST_BLOCK_CHIME;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.AMETHYST_BLOCK_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.AMETHYST_CLUSTER_STEP, 0.3F, 1.2F);
	}
}
