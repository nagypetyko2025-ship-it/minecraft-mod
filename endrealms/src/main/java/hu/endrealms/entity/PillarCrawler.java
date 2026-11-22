package hu.endrealms.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

/**
 * Pillar Crawler: a pale, long-limbed thing of the Listening Pillars that clings to the stone and moves from pillar to
 * pillar above you, then leaps down. It climbs like a spider and hunts in the dark. Strong enough for Chorusite gear.
 */
public class PillarCrawler extends Spider {
	public PillarCrawler(EntityType<? extends PillarCrawler> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Spider.createAttributes()
				.add(Attributes.MAX_HEALTH, 45.0)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.ARMOR, 6.0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SPIDER_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SPIDER_HURT;
	}
}
