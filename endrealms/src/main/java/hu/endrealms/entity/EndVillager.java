package hu.endrealms.entity;

import hu.endrealms.registry.ModItems;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * End villager: pale, horned inhabitant of the End Villages. There is no farmer among them; every other trade has an
 * End counterpart (see {@link Profession}). Trades use chorusite shards instead of emeralds ({@link EndVillagerTrades}).
 * They stay around their house and never despawn; stock refills every ten minutes.
 */
public class EndVillager extends AbstractVillager {
	public enum Profession {
		TOOLSMITH, MASON, BUTCHER, FISHERMAN, CLERIC, LIBRARIAN, FORESTER, JEWELER,
		/** Only ever found on Sky Wrecks. */
		CASTAWAY;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static final EntityDataAccessor<Integer> PROFESSION =
			SynchedEntityData.defineId(EndVillager.class, EntityDataSerializers.INT);
	private static final int RESTOCK_INTERVAL = 12000;

	public EndVillager(EntityType<? extends EndVillager> type, Level level) {
		super(type, level);
		if (this.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanOpenDoors(true);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PROFESSION, -1);
	}

	/** -1 only until the first spawn decides it. */
	public int getProfessionIndex() {
		return this.entityData.get(PROFESSION);
	}

	public Profession getProfession() {
		return Profession.values()[Math.max(0, getProfessionIndex()) % Profession.values().length];
	}

	public void setProfession(Profession profession) {
		this.entityData.set(PROFESSION, profession.ordinal());
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
		if (getProfessionIndex() < 0) {
			setProfession(Profession.values()[this.random.nextInt(Profession.CASTAWAY.ordinal())]);
		}
		this.getOffers(); // decided now, so that the stock is saved with the entity from the start
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 0.5));
		this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
		this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.35));
		this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
		this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof SpawnEggItem || !this.isAlive() || this.isTrading()) {
			return super.mobInteract(player, hand);
		}
		if (hand == InteractionHand.MAIN_HAND) {
			player.awardStat(Stats.TALKED_TO_VILLAGER);
		}
		if (this.level() instanceof ServerLevel && !this.getOffers().isEmpty()) {
			this.setTradingPlayer(player);
			this.openTradingScreen(player, this.getDisplayName(), 1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void updateTrades(ServerLevel level) {
		EndVillagerTrades.fill(getProfession(), this.getOffers());
	}

	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
		if (offer.shouldRewardExp() && this.level() instanceof ServerLevel level) {
			ExperienceOrb.award(level, this.position().add(0.0, 0.5, 0.0), offer.getXp());
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.tickCount % RESTOCK_INTERVAL == 0 && !this.isTrading()) {
			MerchantOffers offers = this.getOffers();
			offers.forEach(MerchantOffer::resetUses);
		}
	}

	@Override
	public boolean showProgressBar() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	protected Component getTypeName() {
		return Component.translatable("entity.endrealms.end_villager." + getProfession().id());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Profession", getProfession().id());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		String id = input.getStringOr("Profession", "");
		for (Profession profession : Profession.values()) {
			if (profession.id().equals(id)) {
				setProfession(profession);
			}
		}
		if (this.level() instanceof ServerLevel) {   // villagers saved before shards stopped being for sale
			this.getOffers().removeIf(offer -> offer.getResult().is(ModItems.CHORUSITE_SHARD));
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public float getVoicePitch() {
		return 0.7F;    // lower and stranger than an Overworld villager
	}
}
