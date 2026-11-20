package hu.endrealms.entity;

import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Glimmerhoof: a long-legged, antennaed grazer of the Astral Forest. Bred and tempted with chorus fruit,
 * drops raw glimmerhoof meat. Spawns only on end soil and, unlike overworld animals, needs no light.
 * Saddled, it can be ridden: hold jump to charge a leap far higher than a horse's, and it lands softly.
 */
public class Glimmerhoof extends Animal implements PlayerRideableJumping {
	private static final float MAX_LEAP = 1.45F;
	private static final float FORWARD_LEAP = 0.7F;
	/** About an average horse: the Glimmerhoof stands out by its leap, not its speed. */
	private static final float RIDDEN_SPEED = 1.05F;
	private static final double SAFE_FALL = 12.0;

	private float pendingLeap;

	public Glimmerhoof(EntityType<? extends Glimmerhoof> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.MOVEMENT_SPEED, 0.22);
	}

	public static boolean checkGlimmerhoofSpawnRules(EntityType<Glimmerhoof> type, ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(ModBlocks.END_SOIL);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
		this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, this::isFood, false));
		this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.CHORUS_FRUIT);
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return ModEntities.GLIMMERHOOF.create(level, EntitySpawnReason.BREEDING);
	}

	// ---------------------------------------------------------------- saddle and riding

	/** A saddle goes on like on a pig: the saddle item does it, the slot only has to accept it. */
	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.SADDLE ? isAlive() && !isBaby() : super.canUseSlot(slot);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (isSaddled() && !isVehicle() && !isFood(held) && !held.is(Items.SADDLE) && !held.is(Items.SHEARS) && !player.isSecondaryUseActive()) {
			if (!level().isClientSide()) {
				player.startRiding(this);
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public LivingEntity getControllingPassenger() {
		if (isSaddled() && getFirstPassenger() instanceof Player player) {
			return player;
		}
		return super.getControllingPassenger();
	}

	@Override
	protected void tickRidden(Player player, Vec3 travel) {
		super.tickRidden(player, travel);
		setRot(player.getYRot(), player.getXRot() * 0.5F);
		this.yRotO = this.yBodyRot = this.yHeadRot = getYRot();
		if (onGround() && pendingLeap > 0.0F) {
			float yaw = getYRot() * Mth.DEG_TO_RAD;
			Vec3 motion = getDeltaMovement();
			double forward = FORWARD_LEAP * pendingLeap;
			setDeltaMovement(motion.x - Mth.sin(yaw) * forward, MAX_LEAP * pendingLeap, motion.z + Mth.cos(yaw) * forward);
			playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.4F);
			pendingLeap = 0.0F;
		}
	}

	@Override
	protected Vec3 getRiddenInput(Player player, Vec3 travel) {
		float forward = player.zza;
		if (forward <= 0.0F) {
			forward *= 0.25F;
		}
		return new Vec3(player.xxa * 0.5F, 0.0, forward);
	}

	@Override
	protected float getRiddenSpeed(Player player) {
		return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * RIDDEN_SPEED;
	}

	/** Called on both sides when the rider lets go of jump; power is 0..100 like a horse's jump bar. */
	@Override
	public void onPlayerJump(int power) {
		pendingLeap = power <= 0 ? 0.0F : power >= 90 ? 1.0F : 0.4F + 0.6F * power / 90.0F;
	}

	@Override
	public boolean canJump() {
		return isSaddled();
	}

	@Override
	public void handleStartJump(int power) {
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.7F);
	}

	@Override
	public void handleStopJump() {
	}

	/** The saddle comes off when it dies, like a pig's. */
	@Override
	protected void dropEquipment(ServerLevel level) {
		super.dropEquipment(level);
		ItemStack saddle = getItemBySlot(EquipmentSlot.SADDLE);
		if (!saddle.isEmpty()) {
			spawnAtLocation(level, saddle.copy());
			setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
		}
	}

	/** Long legs: falls from its own leaps hurt neither it nor (as a passenger) its rider. */
	@Override
	protected int calculateFallDamage(double fallDistance, float multiplier) {
		return super.calculateFallDamage(fallDistance - SAFE_FALL, multiplier);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.AMETHYST_BLOCK_CHIME;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SHULKER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SHULKER_DEATH;
	}
}
