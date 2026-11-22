package hu.endrealms.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Shade: the dark, see-through shape of someone who once lived in the Shadow City, still walking its rounds among the
 * glass ruins. It cannot be hurt and harms no one; come too close and it comes apart like smoke.
 */
public class Shade extends PathfinderMob {
	private static final double VANISH_DISTANCE = 6.0;

	public Shade(EntityType<? extends Shade> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.18);
	}

	public static boolean checkSpawnRules(EntityType<Shade> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return !level.getBlockState(pos.below()).isAir();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.04F));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 24.0F, 0.6F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level && tickCount % 5 == 0
				&& level.getNearestPlayer(this, VANISH_DISTANCE) instanceof Player player && !player.isSpectator()) {
			vanish(level);
		}
	}

	private void vanish(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 30, 0.25, 0.6, 0.25, 0.01);
		level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.2, getZ(), 6, 0.3, 0.5, 0.3, 0.03);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 1.0F, 0.6F);
		discard();
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return true;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SOUL_ESCAPE.value();
	}

	@Override
	public float getVoicePitch() {
		return 0.5F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.6F;
	}
}
