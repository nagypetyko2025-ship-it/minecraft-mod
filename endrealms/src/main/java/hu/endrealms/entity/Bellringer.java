package hu.endrealms.entity;

import hu.endrealms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Bellringer: a tall, gaunt figure of bone with a small cracked bell inside its ribs that rings as it walks, so you
 * hear it coming before you see it. The great bells of the Bell Valley call it to whoever hears them toll. Its blows
 * bring the dark down over your eyes. Strong enough for Chorusite gear.
 */
public class Bellringer extends Monster {
	public Bellringer(EntityType<? extends Bellringer> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 60.0)
				.add(Attributes.ATTACK_DAMAGE, 11.0)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** A great bell tolled: it heads for the player who heard it. */
	public void hearToll(ServerPlayer player) {
		setTarget(player);
		getNavigation().moveTo(player, 1.1);
	}

	/** Called by a toll when too few Bellringers are about: one climbs out of the dark somewhere behind the player. */
	public static void callUp(ServerLevel level, ServerPlayer player, RandomSource random) {
		for (int attempt = 0; attempt < 10; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 24.0 + random.nextDouble() * 16.0;
			int x = (int) (player.getX() + Math.cos(angle) * distance);
			int z = (int) (player.getZ() + Math.sin(angle) * distance);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);
			if (!level.getBlockState(pos.below()).isSolid() || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())
					|| !level.isEmptyBlock(pos.above(2))) {
				continue;
			}
			Bellringer ringer = ModEntities.BELLRINGER.create(level, EntitySpawnReason.EVENT);
			if (ringer != null) {
				ringer.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
				level.addFreshEntity(ringer);
				ringer.hearToll(player);
			}
			return;
		}
	}

	/** The little bell in its ribs rings with its steps. */
	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && tickCount % 24 == 0 && getDeltaMovement().horizontalDistanceSqr() > 1.0E-3) {
			playSound(SoundEvents.BELL_BLOCK, 0.35F, 1.4F + getRandom().nextFloat() * 0.2F);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120), this);
			playSound(SoundEvents.BELL_RESONATE, 1.0F, 0.7F);
		}
		return hit;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.4F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BELL_RESONATE;
	}

	@Override
	protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		playSound(SoundEvents.SKELETON_STEP, 0.3F, 0.6F);
	}
}
