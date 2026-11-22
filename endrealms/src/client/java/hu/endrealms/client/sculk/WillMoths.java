package hu.endrealms.client.sculk;

import hu.endrealms.sculk.SculkDimension;
import hu.endrealms.sculk.SculkParticles;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Will-moths: little pale glowing moths that flutter in slow circles around any light in the Sculk dimension - its
 * Soul Mushrooms, or your torches. Pure decoration: drawn only on your own screen, they cannot be caught and leave
 * the lights alone.
 */
public final class WillMoths {
	private static final int SCAN_RADIUS = 14;
	private static final int SCAN_HEIGHT = 6;
	private static final int MIN_LIGHT = 8;
	private static final List<BlockPos> LIGHTS = new ArrayList<>();

	private WillMoths() {
	}

	public static void init() {
		ParticleProviderRegistry.getInstance().register(SculkParticles.WILL_MOTH, sprites -> new Provider(sprites));
		ClientTickEvents.END_CLIENT_TICK.register(WillMoths::tick);
	}

	private static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null || minecraft.isPaused() || level.dimension() != SculkDimension.LEVEL) {
			LIGHTS.clear();
			return;
		}
		RandomSource random = level.getRandom();
		if (level.getGameTime() % 40 == 0) {                 // look for lights around every two seconds
			LIGHTS.clear();
			BlockPos center = minecraft.player.blockPosition();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
				for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
					for (int dy = -SCAN_HEIGHT; dy <= SCAN_HEIGHT; dy++) {
						if (level.getBlockState(pos.setWithOffset(center, dx, dy, dz)).getLightEmission() >= MIN_LIGHT) {
							LIGHTS.add(pos.immutable());
						}
					}
				}
			}
		}
		for (BlockPos light : LIGHTS) {                       // about four moths around each light at a time
			if (random.nextInt(60) == 0) {
				level.addParticle(SculkParticles.WILL_MOTH, light.getX() + 0.5, light.getY() + 0.6, light.getZ() + 0.5, 0.0, 0.0, 0.0);
			}
		}
	}

	private record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
				double dx, double dy, double dz, RandomSource random) {
			return new Moth(level, x, y, z, sprites, random);
		}
	}

	/** One moth: circles its light at its own radius, height and speed, bobbing and flickering, wings beating. */
	static final class Moth extends SingleQuadParticle {
		private final SpriteSet sprites;
		private final double cx;
		private final double cy;
		private final double cz;
		private final double radius;
		private final double height;
		private final float speed;
		private float angle;

		Moth(ClientLevel level, double x, double y, double z, SpriteSet sprites, RandomSource random) {
			super(level, x, y, z, sprites.first());
			this.sprites = sprites;
			this.cx = x;
			this.cy = y;
			this.cz = z;
			this.radius = 0.5 + random.nextDouble() * 0.9;
			this.height = random.nextDouble() * 1.2;
			this.speed = (0.05F + random.nextFloat() * 0.06F) * (random.nextBoolean() ? 1 : -1);
			this.angle = random.nextFloat() * Mth.TWO_PI;
			this.lifetime = 200 + random.nextInt(200);
			this.quadSize = 0.06F + random.nextFloat() * 0.03F;
			this.hasPhysics = false;
			this.gravity = 0.0F;
			setColor(0.80F, 0.95F, 1.0F);
			place();
			this.xo = this.x;
			this.yo = this.y;
			this.zo = this.z;
		}

		private void place() {
			this.x = cx + Math.cos(angle) * radius;
			this.z = cz + Math.sin(angle) * radius;
			this.y = cy + height + Math.sin(angle * 2.3 + age * 0.15) * 0.15;
		}

		@Override
		public void tick() {
			this.xo = this.x;
			this.yo = this.y;
			this.zo = this.z;
			if (this.age++ >= this.lifetime) {
				remove();
				return;
			}
			angle += speed;
			place();
			setSprite(sprites.get((age / 2) % 2, 1));          // the wings beat
			float fade = Math.min(1.0F, Math.min(age / 20.0F, (lifetime - age) / 20.0F));
			setAlpha(fade * (0.65F + 0.35F * Mth.sin(age * 0.4F)));
		}

		@Override
		public int getLightCoords(float partialTick) {
			return 0xF000F0;
		}

		@Override
		protected Layer getLayer() {
			return Layer.TRANSLUCENT;
		}
	}
}
