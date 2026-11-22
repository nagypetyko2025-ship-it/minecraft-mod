package hu.endrealms.entity;

import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.sculk.SculkPlants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Bone Stag: a gaunt deer of the Sculk dimension, half skeleton, with sculk hanging from its ribs - you can see
 * through them. It stands still and stares at you from far off, and slips away once you come close. Soul Mushrooms
 * lure it and make it breed; it drops a Sculk Antler and bones. Harmless, so its strength has nothing to do with the
 * dimension's monsters.
 */
public class BoneStag extends Animal {
	/** How close you may come before it turns and goes. */
	private static final float SHY_DISTANCE = 9.0F;
	/** How far off it notices you and stares. */
	private static final float STARE_DISTANCE = 40.0F;

	public BoneStag(EntityType<? extends BoneStag> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 16.0)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.FOLLOW_RANGE, STARE_DISTANCE);
	}

	/** On the ground of the Sculk dimension, in any light: there is none to speak of. */
	public static boolean checkSpawnRules(EntityType<BoneStag> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return SculkPlants.ground(level.getBlockState(pos.below()));
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(2, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, SHY_DISTANCE, 1.1, 1.5,
				player -> !((Player) player).isCreative() && !isFood(((Player) player).getMainHandItem()) && !isFood(((Player) player).getOffhandItem())));
		this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
		// the stare outranks wandering: once it has seen you, it stands and watches
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, STARE_DISTANCE, 1.0F) {
			@Override
			public boolean canContinueToUse() {
				return super.canContinueToUse() && !BoneStag.this.isPathFinding();
			}
		});
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6, 0.002F));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(ModBlocks.SOUL_MUSHROOM.asItem());
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return ModEntities.BONE_STAG.create(level, EntitySpawnReason.BREEDING);
	}

	/** Mostly silent; now and then a dry rattle of bone. */
	@Override
	public int getAmbientSoundInterval() {
		return 400;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.55F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		playSound(SoundEvents.SKELETON_STEP, 0.15F, 0.7F);
	}
}
