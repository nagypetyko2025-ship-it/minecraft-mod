package hu.endrealms.entity;

import hu.endrealms.registry.ModItems;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bone Drake: a small dragon that sits in a nest at the heart of every Dragon Graveyard and cannot be hurt while wild.
 * Fed Dragon Bones it may take to the player; from then on it flies after them like a dog, fights what they fight,
 * and sits or stands when right-clicked with an empty hand. Dragon Bones heal it.
 */
public class BoneDrake extends TamableAnimal {
	private static final float HEAL = 8.0F;
	public float flap;
	public float flapSpeed;
	private float nextFlap = 1.0F;

	public BoneDrake(EntityType<? extends BoneDrake> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 10, true);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new WildRestGoal());
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(4, new FlyAfterOwnerGoal());
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	/** While wild it keeps to its nest: no wandering, no following, just watching whoever comes near. */
	private final class WildRestGoal extends Goal {
		WildRestGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return !isTame();
		}

		@Override
		public void start() {
			getNavigation().stop();
			setInSittingPose(true);
		}

		@Override
		public void stop() {
			setInSittingPose(false);
		}
	}

	/**
	 * Tame and not told to sit: it keeps flying a little behind and above its owner's shoulder, and catches up (or
	 * teleports, if far) whenever the owner moves on. Fights take over through the attack goal's higher priority.
	 */
	private final class FlyAfterOwnerGoal extends Goal {
		private static final double HEIGHT = 2.0;

		FlyAfterOwnerGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity owner = getOwner();
			return isTame() && !isOrderedToSit() && owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == level() && !isLeashed();
		}

		@Override
		public void tick() {
			LivingEntity owner = getOwner();
			if (owner == null) {
				return;
			}
			getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
			if (shouldTryTeleportToOwner()) {
				tryToTeleportToOwner();
				return;
			}
			float yaw = owner.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
			double side = 1.2;
			double back = 1.6;
			double tx = owner.getX() + net.minecraft.util.Mth.sin(yaw) * back + net.minecraft.util.Mth.cos(yaw) * side;
			double tz = owner.getZ() - net.minecraft.util.Mth.cos(yaw) * back + net.minecraft.util.Mth.sin(yaw) * side;
			double ty = owner.getY() + HEIGHT;
			double distance = Math.sqrt(distanceToSqr(tx, ty, tz));
			if (distance > 0.8) {
				getMoveControl().setWantedPosition(tx, ty, tz, distance > 6.0 ? 1.8 : distance > 2.5 ? 1.2 : 0.7);
			}
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean bone = stack.is(ModItems.DRAGON_BONE);
		if (!isTame()) {
			if (!bone) {
				return InteractionResult.PASS;
			}
			if (level() instanceof ServerLevel) {
				stack.consume(1, player);
				if (random.nextInt(3) == 0) {
					tame(player);
					setOrderedToSit(false);
					setInSittingPose(false);
					navigation.stop();
					setTarget(null);
					level().broadcastEntityEvent(this, (byte) 7);
				} else {
					level().broadcastEntityEvent(this, (byte) 6);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (!isOwnedBy(player)) {
			return super.mobInteract(player, hand);
		}
		if (bone && getHealth() < getMaxHealth()) {
			if (level() instanceof ServerLevel) {
				stack.consume(1, player);
				heal(HEAL);
				playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.4F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.isEmpty()) {
			if (level() instanceof ServerLevel) {
				setOrderedToSit(!isOrderedToSit());
				setInSittingPose(isOrderedToSit());
				navigation.stop();
				setTarget(null);
				jumping = false;
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** Wild drakes are the graveyard's guardians of nothing: they cannot be hurt until tamed (the void and creative players still can). */
	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		if (!isTame() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}
		return super.isInvulnerableTo(level, source);
	}

	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof TamableAnimal other && other.isTame() && other.getOwner() == owner) {
			return false;
		}
		return super.wantsToAttack(target, owner);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(ModItems.DRAGON_BONE);
	}

	@Override
	public boolean canMate(Animal other) {
		return false;
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	public boolean isFlying() {
		return !onGround();
	}

	@Override
	protected boolean canFlyToOwner() {
		return true;
	}

	/**
	 * The game only teleports a pet onto solid ground near its owner, which a flying owner (or one over the End's void)
	 * does not have. A drake flies, so when that finds no spot it takes any free spot in the air beside them.
	 */
	@Override
	public void tryToTeleportToOwner() {
		super.tryToTeleportToOwner();
		LivingEntity owner = getOwner();
		if (owner == null || distanceToSqr(owner) < 144.0) {
			return;
		}
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double x = owner.getX() + Math.cos(angle) * (1.5 + random.nextDouble());
			double y = owner.getY() + 1.0 + random.nextInt(2);
			double z = owner.getZ() + Math.sin(angle) * (1.5 + random.nextDouble());
			if (level().noCollision(this, getBoundingBox().move(x - getX(), y - getY(), z - getZ()))) {
				snapTo(x, y, z, getYRot(), getXRot());
				navigation.stop();
				return;
			}
		}
	}

	/** Where each player was last tick, to notice a long jump (teleport, Phase Pearl, other dimension or world). */
	private static final java.util.Map<java.util.UUID, net.minecraft.world.level.storage.LevelData.RespawnData> LAST = new java.util.HashMap<>();

	/**
	 * When a player suddenly lands far away, their drakes that were following them come along, even into another dimension
	 * or world: left behind, they would stand in chunks that stop ticking and never catch up.
	 */
	public static void registerFollowing() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			java.util.Set<java.util.UUID> online = new java.util.HashSet<>();
			for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
				online.add(player.getUUID());
				var here = net.minecraft.world.level.storage.LevelData.RespawnData.of(player.level().dimension(), player.blockPosition(), 0.0F, 0.0F);
				var before = LAST.put(player.getUUID(), here);
				if (before == null || before.dimension() == here.dimension() && before.pos().distSqr(here.pos()) < 24 * 24) {
					continue;
				}
				ServerLevel old = server.getLevel(before.dimension());
				if (old == null) {
					continue;
				}
				net.minecraft.world.phys.Vec3 from = net.minecraft.world.phys.Vec3.atCenterOf(before.pos());
				for (BoneDrake drake : old.getEntitiesOfClass(BoneDrake.class, net.minecraft.world.phys.AABB.ofSize(from, 96, 96, 96),
						d -> d.isAlive() && d.isOwnedBy(player) && !d.isOrderedToSit() && !d.isLeashed())) {
					drake.navigation.stop();
					drake.teleport(new net.minecraft.world.level.portal.TeleportTransition(player.level(), player.position().add(0.0, 1.5, 0.0),
							net.minecraft.world.phys.Vec3.ZERO, drake.getYRot(), drake.getXRot(), net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
				}
			}
			LAST.keySet().retainAll(online);
		});
	}

	@Override
	protected void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!level().isClientSide() && (!isTame() || isOrderedToSit() || isLeashed() || getOwner() == null) && isNoGravity() && getTarget() == null) {
			setNoGravity(false);       // hovering flight is only for following: sitting (and wild) drakes come down
		}
		// wing beats, used by the model; the sound only when it actually flies
		flapSpeed += (onGround() || isInSittingPose() ? -0.3F : 0.6F) * 0.3F;
		flapSpeed = Math.clamp(flapSpeed, 0.0F, 1.0F);
		flap += flapSpeed * 0.45F;
		if (!onGround() && getDeltaMovement().y < 0.0) {
			setDeltaMovement(getDeltaMovement().multiply(1.0, 0.7, 1.0));      // glides instead of dropping like a stone
		}
		if (level().isClientSide() && !onGround() && flap > nextFlap) {
			nextFlap = flap + (float) (Math.PI * 2);
			level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, getSoundSource(), 0.15F, 1.6F + random.nextFloat() * 0.3F, false);
		}
		if (level().isClientSide() && !isTame() && random.nextInt(40) == 0) {
			level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENDER_DRAGON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENDER_DRAGON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PHANTOM_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 0.3F;
	}

	@Override
	public float getVoicePitch() {
		return 1.7F + (random.nextFloat() - 0.5F) * 0.2F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}
}
