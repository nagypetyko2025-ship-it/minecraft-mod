package hu.endrealms.brew;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A brandy: its own effects, nausea for just as long, and Drunkenness. Drinking another while still drunk raises
 * Drunkenness by one level, up to IV, and starts its time over.
 * <p>
 * Aged brandy is better ({@link Brewing#QUALITY}): quality 2 lasts half again as long and its first effect that has levels
 * is one level higher; quality 3 lasts twice as long and every effect that has levels is one level higher.
 */
public class BrandyItem extends Item {
	/** Effects where a higher level does nothing: aging only makes these last longer. */
	private static final Set<Holder<MobEffect>> NO_LEVELS = Set.of(MobEffects.NIGHT_VISION, MobEffects.FIRE_RESISTANCE,
			MobEffects.SLOW_FALLING, MobEffects.WATER_BREATHING, MobEffects.INVISIBILITY, MobEffects.LEVITATION, MobEffects.INSTANT_HEALTH);
	/** How far and how long (at quality 1) a revealing brandy makes monsters glow. */
	private static final double REVEAL_RANGE = 32.0;
	private static final int REVEAL_TICKS = 20 * 30;

	private final Supplier<List<MobEffectInstance>> effects;
	/** Whether drinking it also makes the monsters around glow, so they show through walls. */
	private final boolean reveals;

	public BrandyItem(Properties properties, Supplier<List<MobEffectInstance>> effects) {
		this(properties, effects, false);
	}

	public BrandyItem(Properties properties, Supplier<List<MobEffectInstance>> effects, boolean reveals) {
		super(properties);
		this.effects = effects;
		this.reveals = reveals;
	}

	private static int revealTicks(int quality) {
		return Math.round(REVEAL_TICKS * timeFactor(quality));
	}

	/** Every monster within range glows for a while: seen through walls by everyone. */
	private static void revealMonsters(LivingEntity drinker, int quality) {
		net.minecraft.world.phys.AABB box = drinker.getBoundingBox().inflate(REVEAL_RANGE);
		for (LivingEntity monster : drinker.level().getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof net.minecraft.world.entity.monster.Enemy)) {
			monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, revealTicks(quality)), drinker);
		}
	}

	private static float timeFactor(int quality) {
		return quality >= 3 ? 2.0F : quality == 2 ? 1.5F : 1.0F;
	}

	/** The brandy's own effects at this quality. */
	public List<MobEffectInstance> effects(int quality) {
		List<MobEffectInstance> list = new ArrayList<>();
		boolean firstLeveled = true;
		for (MobEffectInstance effect : effects.get()) {
			int amplifier = effect.getAmplifier();
			if (!NO_LEVELS.contains(effect.getEffect())) {
				if (quality >= 3 || quality == 2 && firstLeveled) {
					amplifier++;
				}
				firstLeveled = false;
			}
			list.add(new MobEffectInstance(effect.getEffect(), Math.round(effect.getDuration() * timeFactor(quality)), amplifier));
		}
		return list;
	}

	/** Nausea lasts as long as the brandy's buffs. */
	public static int nausea(int quality) {
		return Math.round(Brewing.DURATION * timeFactor(quality));
	}

	@Override
	public Component getName(ItemStack stack) {
		int quality = Brewing.quality(stack);
		return quality == 1 ? super.getName(stack) : Component.translatable(getDescriptionId() + ".quality_" + quality);
	}

	/** Lists what a sip does, like a potion: its own effects, then the nausea and the Drunkenness that come with every brandy. */
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
			java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
		int quality = Brewing.quality(stack);
		lines.accept(Component.translatable("brandy.endrealms.quality", Component.literal("★".repeat(quality)).withStyle(ChatFormatting.GOLD)
				.append(Component.literal("★".repeat(Brewing.MAX_QUALITY - quality)).withStyle(ChatFormatting.DARK_GRAY))).withStyle(ChatFormatting.GRAY));
		List<MobEffectInstance> all = new ArrayList<>(effects(quality));
		all.add(new MobEffectInstance(MobEffects.NAUSEA, nausea(quality)));
		all.add(new MobEffectInstance(Brewing.DRUNKENNESS, Brewing.DURATION));
		net.minecraft.world.item.alchemy.PotionContents.addPotionTooltip(all, lines, 1.0F, 20.0F);
		if (reveals) {
			lines.accept(Component.translatable("brandy.endrealms.reveal", revealTicks(quality) / 20).withStyle(ChatFormatting.BLUE));
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		int quality = Brewing.quality(stack);     // read before the stack is used up
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (!level.isClientSide()) {
			for (MobEffectInstance effect : effects(quality)) {
				entity.addEffect(effect);
			}
			entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, nausea(quality)));
			if (reveals) {
				revealMonsters(entity, quality);
			}
			MobEffectInstance drunk = entity.getEffect(Brewing.DRUNKENNESS);
			int level2 = drunk == null ? 0 : Math.min(Brewing.MAX_DRUNK_LEVEL, drunk.getAmplifier() + 1);
			entity.removeEffect(Brewing.DRUNKENNESS);     // so that the time starts over even at the top level
			entity.addEffect(new MobEffectInstance(Brewing.DRUNKENNESS, Brewing.DURATION, level2));
		}
		return result;
	}
}
