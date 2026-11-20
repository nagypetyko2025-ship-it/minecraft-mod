package hu.endrealms.entity;

import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModEntities;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rift Lord: the boss of the Rift Sanctum. It stands still until it sees a player, then fights in three phases:
 * melee, stepping behind its target and opening telegraphed rifts under the players' feet; from 60% it calls two Void
 * Sentinels and takes half damage while they live; from 25% it speeds up and drags everyone towards itself.
 * Losing every target for five seconds sends it home at full health.
 */
public class RiftLord extends Monster {
	private static final ResourceKey<LootTable> REWARD = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/rift_sanctum"));
	private static final double BASE_SPEED = 0.30;
	private static final double ENRAGED_SPEED = 0.38;
	private static final double ARENA_RADIUS = 16.0;
	private static final double LEASH = 34.0;
	private static final double ARENA_HEIGHT = 12.0;
	private static final double BAR_RANGE = 40.0;
	private static final int RIFT_FUSE = 30;
	private static final int PULL_CHARGE = 30;

	private static final class Rift {
		final Vec3 pos;
		int fuse = RIFT_FUSE;

		Rift(Vec3 pos) {
			this.pos = pos;
		}
	}

	protected final ServerBossEvent bossEvent;
	private final List<Rift> rifts = new ArrayList<>();
	private BlockPos home;
	private boolean guardian;
	private boolean awake;
	private int phase = 1;
	private int stepTimer = 90;
	private int riftTimer = 50;
	private int pullTimer = 100;
	private int pullCharge = -1;
	private int calmTicks;

	public RiftLord(EntityType<? extends RiftLord> type, Level level) {
		super(type, level);
		this.bossEvent = new ServerBossEvent(UUID.randomUUID(), type.getDescription(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
		this.xpReward = 500;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 450.0)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.ARMOR, 14.0)
				.add(Attributes.ARMOR_TOUGHNESS, 6.0)
				.add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/** Called by the structure: this boss guards an arena, returns to its centre and leaves a reward chest there. */
	public void guardArena(BlockPos centre) {
		this.home = centre;
		this.guardian = true;
		this.setPersistenceRequired();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	protected ResourceKey<LootTable> rewardTable() {
		return REWARD;
	}

	/** X offsets from the arena centre at which Void Sentinels appear when the shield phase starts. */
	protected int[] sentinelOffsets() {
		return new int[] {-7, 7};
	}

	/** Whether a point belongs to the guarded arena; the fight never reaches anything outside it. */
	protected boolean inArena(double x, double y, double z) {
		double dx = x - (home.getX() + 0.5);
		double dz = z - (home.getZ() + 0.5);
		return dx * dx + dz * dz < ARENA_RADIUS * ARENA_RADIUS && Math.abs(y - home.getY()) < ARENA_HEIGHT;
	}

	protected BlockPos arenaCentre() {
		return home;
	}

	protected boolean isGuardian() {
		return guardian;
	}

	protected boolean isAwake() {
		return awake;
	}

	protected int phase() {
		return phase;
	}

	// ---------------------------------------------------------------- fight

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (home == null) {
			home = blockPosition();
		}
		bossEvent.setProgress(getHealth() / getMaxHealth());
		if (tickCount % 20 == 0) {
			updateBar(level);
		}
		tickRifts(level);
		LivingEntity target = getTarget();
		if (target != null && (!target.isAlive() || !inFight(target))) {
			setTarget(null);
			target = null;
		}
		if (target == null) {
			if (awake && ++calmTicks > 100) {
				reset();
			}
			return;
		}
		calmTicks = 0;
		if (!awake) {
			awake = true;
			level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0F, 0.7F);
			updateBar(level);
		}
		if (position().distanceToSqr(Vec3.atBottomCenterOf(home)) > LEASH * LEASH) {
			effects(level);
			teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
			effects(level);
		}
		float health = getHealth() / getMaxHealth();
		int wanted = health <= 0.25F ? 3 : health <= 0.6F ? 2 : 1;
		if (wanted > phase) {
			phase = wanted;
			enterPhase(level, target);
		}
		if (--stepTimer <= 0 && stepBehind(level, target)) {
			stepTimer = 130 - phase * 25;
		}
		if (--riftTimer <= 0) {
			riftTimer = 110 - phase * 20;
			openRifts(target);
		}
		if (phase >= 3) {
			tickPull(level);
		}
	}

	/** A guardian only fights what is inside its arena; anyone on the stairs or beyond is out of reach, walls or not. */
	protected boolean inFight(LivingEntity entity) {
		if (!guardian) {
			return entity.distanceToSqr(Vec3.atCenterOf(home)) <= LEASH * LEASH;
		}
		return inArena(entity.getX(), entity.getY(), entity.getZ());
	}

	private void enterPhase(ServerLevel level, LivingEntity target) {
		level.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 1.4F);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2.0, getZ(), 150, 1.2, 1.5, 1.2, 0.15);
		if (phase == 2) {
			for (int offset : sentinelOffsets()) {
				summonSentinel(level, target, offset);
			}
			announce("shield");
		} else {
			getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(ENRAGED_SPEED);
			announce("enraged");
		}
	}

	private void announce(String key) {
		for (ServerPlayer player : bossEvent.getPlayers()) {
			player.sendSystemMessage(Component.translatable(getType().getDescriptionId() + "." + key), true);
		}
	}

	private void summonSentinel(ServerLevel level, LivingEntity target, int dx) {
		VoidSentinel sentinel = ModEntities.VOID_SENTINEL.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (sentinel == null) {
			return;
		}
		BlockPos pos = home.offset(dx, 0, 0);
		sentinel.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		sentinel.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
		sentinel.setTarget(target);
		level.addFreshEntity(sentinel);
		level.sendParticles(ParticleTypes.PORTAL, sentinel.getX(), sentinel.getY() + 1.2, sentinel.getZ(), 60, 0.5, 1.0, 0.5, 0.3);
	}

	/** Reappears a few blocks behind the target; false (retried next tick) if no spot there is free. */
	private boolean stepBehind(ServerLevel level, LivingEntity target) {
		float yaw = target.getYRot() * Mth.DEG_TO_RAD;
		for (int attempt = 0; attempt < 10; attempt++) {
			float angle = yaw + (random.nextFloat() - 0.5F) * 1.6F;
			double distance = 2.5 + random.nextDouble() * 1.5;
			double x = target.getX() + Mth.sin(angle) * distance;
			double z = target.getZ() - Mth.cos(angle) * distance;
			AABB box = getBoundingBox().move(x - getX(), target.getY() - getY(), z - getZ());
			if ((!guardian || inArena(x, home.getY(), z)) && level.noCollision(this, box) && !level.noCollision(this, box.move(0.0, -0.5, 0.0))) {
				effects(level);
				teleportTo(x, target.getY(), z);
				getNavigation().stop();
				effects(level);
				return true;
			}
		}
		return false;
	}

	private void effects(ServerLevel level) {
		level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.7, getZ(), 50, 0.6, 1.2, 0.6, 0.25);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.6F);
	}

	private void openRifts(LivingEntity target) {
		rifts.add(new Rift(target.position()));
		if (phase >= 2) {
			for (int i = 0; i < 2; i++) {
				rifts.add(new Rift(target.position().add(random.nextDouble() * 8.0 - 4.0, 0.0, random.nextDouble() * 8.0 - 4.0)));
			}
		}
	}

	private void tickRifts(ServerLevel level) {
		Iterator<Rift> iterator = rifts.iterator();
		while (iterator.hasNext()) {
			Rift rift = iterator.next();
			if (--rift.fuse % 3 == 0) {
				for (int i = 0; i < 14; i++) {
					float a = i * Mth.TWO_PI / 14.0F;
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, rift.pos.x + Mth.cos(a) * 2.2, rift.pos.y + 0.1, rift.pos.z + Mth.sin(a) * 2.2, 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
			if (rift.fuse <= 0) {
				iterator.remove();
				level.sendParticles(ParticleTypes.EXPLOSION, rift.pos.x, rift.pos.y + 0.8, rift.pos.z, 3, 0.8, 0.5, 0.8, 0.0);
				level.playSound(null, rift.pos.x, rift.pos.y, rift.pos.z, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 1.2F, 0.8F);
				for (Player player : level.getEntitiesOfClass(Player.class, AABB.ofSize(rift.pos, 4.6, 5.0, 4.6))) {
					if (inFight(player) && player.position().distanceToSqr(rift.pos.x, player.getY(), rift.pos.z) <= 2.3 * 2.3) {
						player.hurtServer(level, damageSources().indirectMagic(this, this), phase >= 2 ? 10.0F : 8.0F);
					}
				}
			}
		}
	}

	private void tickPull(ServerLevel level) {
		if (pullCharge < 0) {
			if (--pullTimer <= 0) {
				pullCharge = PULL_CHARGE;
				level.playSound(null, blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.5F, 1.5F);
			}
			return;
		}
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.7, getZ(), 12, 3.0, 1.0, 3.0, 0.4);
		if (--pullCharge >= 0) {
			return;
		}
		pullTimer = 140;
		level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 2.0F, 0.5F);
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(22.0), this::inFight)) {
			Vec3 towards = position().subtract(player.position());
			double distance = towards.length();
			if (distance < 5.0) {
				player.hurtServer(level, damageSources().indirectMagic(this, this), 10.0F);
			} else if (!player.isSpectator() && !player.getAbilities().flying) {
				player.setDeltaMovement(towards.normalize().scale(Math.min(2.2, distance * 0.16)).add(0.0, 0.35, 0.0));
				player.syncVelocity = true;
			}
		}
	}

	private void updateBar(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (awake && player.distanceToSqr(this) < BAR_RANGE * BAR_RANGE) {
				bossEvent.addPlayer(player);
			} else {
				bossEvent.removePlayer(player);
			}
		}
	}

	private void reset() {
		awake = false;
		calmTicks = 0;
		phase = 1;
		pullCharge = -1;
		rifts.clear();
		setHealth(getMaxHealth());
		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(BASE_SPEED);
		bossEvent.removeAllPlayers();
		teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
		getNavigation().stop();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL)) {
			return false;
		}
		if (phase >= 2 && !level().getEntitiesOfClass(VoidSentinel.class, getBoundingBox().inflate(32.0)).isEmpty()) {
			amount *= 0.5F;
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 1.7, getZ(), 12, 0.6, 1.0, 0.6, 0.1);
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public void die(DamageSource source) {
		boolean first = !this.dead;
		super.die(source);
		bossEvent.removeAllPlayers();
		rifts.clear();
		if (first && guardian && level() instanceof ServerLevel level && level.getBlockState(home).canBeReplaced()) {
			level.setBlock(home, Blocks.CHEST.defaultBlockState(), 3);
			RandomizableContainer.setBlockEntityLootTable(level, random, home, rewardTable());
			level.sendParticles(ParticleTypes.END_ROD, home.getX() + 0.5, home.getY() + 1.0, home.getZ() + 0.5, 60, 0.6, 1.0, 0.6, 0.08);
		}
	}

	// ---------------------------------------------------------------- bookkeeping

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			level().addParticle(ParticleTypes.PORTAL, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0.0, -0.3, 0.0);
		}
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		super.remove(reason);
		bossEvent.removeAllPlayers();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	/** Peaceful difficulty would otherwise delete the boss of the structure for good. */
	@Override
	public void checkDespawn() {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Guardian", guardian);
		output.putBoolean("HasHome", home != null);
		if (home != null) {
			output.putInt("HomeX", home.getX());
			output.putInt("HomeY", home.getY());
			output.putInt("HomeZ", home.getZ());
		}
		output.putInt("Phase", phase);
		output.putBoolean("Awake", awake);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		guardian = input.getBooleanOr("Guardian", false);
		if (input.getBooleanOr("HasHome", false)) {
			home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
		}
		phase = input.getIntOr("Phase", 1);
		awake = input.getBooleanOr("Awake", false);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENDERMAN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENDERMAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.45F;
	}
}
