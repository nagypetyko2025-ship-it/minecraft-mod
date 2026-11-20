package hu.endrealms.entity;

import hu.endrealms.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Shared class for the three Astral Basin fish (Starfin, Void Eel, Glowgill); they differ in model and loot. */
public class AstralFish extends AbstractFish {
	public AstralFish(EntityType<? extends AstralFish> type, Level level) {
		super(type, level);
	}

	/** Only in astral water (source blocks), so they appear in the Astral Basin lakes. */
	public static boolean checkAstralFishSpawnRules(EntityType<AstralFish> type, ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getFluidState(pos).getType() == ModFluids.ASTRAL_WATER
				&& level.getFluidState(pos.above()).getType() == ModFluids.ASTRAL_WATER;
	}

	/** No bucket for these fish (yet): interacting does nothing instead of producing an empty bucket. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

	@Override
	public ItemStack getBucketItemStack() {
		return ItemStack.EMPTY;
	}

	@Override
	protected SoundEvent getFlopSound() {
		return SoundEvents.TROPICAL_FISH_FLOP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.TROPICAL_FISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.TROPICAL_FISH_DEATH;
	}
}
