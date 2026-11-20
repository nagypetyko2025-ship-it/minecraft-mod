package hu.endrealms.entity;

import hu.endrealms.EndRealms;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * Void King: the boss in the throne room of the Void Citadel. Fights like the Rift Lord, a little harder, and adds
 * ground shockwaves that have to be jumped over. Its death also opens the sealed stair to the treasury under the hall.
 */
public class VoidKing extends RiftLord {
	private static final ResourceKey<LootTable> REWARD = ResourceKey.create(Registries.LOOT_TABLE, EndRealms.id("chests/void_king"));
	private static final float WAVE_SPEED = 0.55F;
	private static final float WAVE_END = 20.0F;
	private static final double WAVE_WIDTH = 1.1;
	private static final float WAVE_DAMAGE = 9.0F;
	private static final int WAVE_WARNING = 20;

	private final Set<UUID> waveHit = new HashSet<>();
	private int waveTimer = 120;
	private float waveRadius = -1.0F;
	private int waveCharge = -1;

	public VoidKing(EntityType<? extends VoidKing> type, Level level) {
		super(type, level);
		this.xpReward = 800;
		this.bossEvent.setColor(BossEvent.BossBarColor.RED);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return RiftLord.createAttributes()
				.add(Attributes.MAX_HEALTH, 600.0)
				.add(Attributes.ATTACK_DAMAGE, 16.0)
				.add(Attributes.ARMOR, 16.0);
	}

	@Override
	protected ResourceKey<LootTable> rewardTable() {
		return REWARD;
	}

	@Override
	protected int[] sentinelOffsets() {
		return new int[] {-9, 0, 9};
	}

	/** The throne room: 31 wide and 26 deep, with its centre one block south of the middle row. */
	@Override
	protected boolean inArena(double x, double y, double z) {
		BlockPos centre = arenaCentre();
		double dx = x - (centre.getX() + 0.5);
		double dz = z - (centre.getZ() + 0.5);
		return Math.abs(dx) < 15.5 && dz > -13.5 && dz < 12.5 && y >= centre.getY() - 1 && y < centre.getY() + 15;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!isAwake()) {
			waveRadius = -1.0F;
			waveCharge = -1;
			return;
		}
		if (getTarget() != null || waveRadius >= 0.0F) {
			tickWave(level);
		}
	}

	private void tickWave(ServerLevel level) {
		if (waveRadius < 0.0F) {
			if (waveCharge >= 0) {
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2, getZ(), 10, 1.2, 0.1, 1.2, 0.02);
				if (--waveCharge < 0) {
					waveRadius = 1.5F;
					waveHit.clear();
					level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4F, 0.8F);
				}
			} else if (--waveTimer <= 0) {
				waveCharge = WAVE_WARNING;
				level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0F, 1.0F);
			}
			return;
		}
		waveRadius += WAVE_SPEED;
		if (tickCount % 2 == 0) {
			int points = (int) (waveRadius * 4.0F) + 8;
			for (int i = 0; i < points; i++) {
				float angle = i * Mth.TWO_PI / points;
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Mth.cos(angle) * waveRadius, getY() + 0.2, getZ() + Mth.sin(angle) * waveRadius, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}
		AABB reach = AABB.ofSize(position(), waveRadius * 2.0F + 4.0F, 6.0, waveRadius * 2.0F + 4.0F);
		for (Player player : level.getEntitiesOfClass(Player.class, reach, this::inFight)) {
			double distance = Math.sqrt(player.distanceToSqr(getX(), player.getY(), getZ()));
			if (player.onGround() && Math.abs(distance - waveRadius) < WAVE_WIDTH && waveHit.add(player.getUUID())) {
				player.hurtServer(level, damageSources().indirectMagic(this, this), WAVE_DAMAGE);
				player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.55, 0.0));
				player.syncVelocity = true;
			}
		}
		if (waveRadius > WAVE_END) {
			waveRadius = -1.0F;
			waveTimer = 190 - phase() * 30;
		}
	}

	@Override
	public void die(DamageSource source) {
		boolean first = !this.dead;
		if (first && isGuardian() && level() instanceof ServerLevel level) {
			level.setBlock(arenaCentre(), Blocks.AIR.defaultBlockState(), 3);    // the carpet would keep the reward chest out
		}
		super.die(source);
		if (first && isGuardian() && level() instanceof ServerLevel level) {
			BlockPos centre = arenaCentre();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -8; dz <= -6; dz++) {
					level.setBlock(centre.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
					level.setBlock(centre.offset(dx, -1, dz), Blocks.AIR.defaultBlockState(), 3);
				}
			}
			BlockPos seal = centre.offset(0, 0, -7);
			level.playSound(null, seal, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.5F, 1.0F);
			level.sendParticles(ParticleTypes.END_ROD, seal.getX() + 0.5, seal.getY() + 0.5, seal.getZ() + 0.5, 80, 1.2, 0.5, 1.2, 0.06);
		}
	}
}
