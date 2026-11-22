package hu.endrealms.client.sculk;

import hu.endrealms.EndRealms;
import hu.endrealms.sculk.SculkDimension;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

/**
 * Sounds that only you hear, in two of the Sculk dimension's lands. Among the Listening Pillars the still air hums like
 * a vast organ: slow, low notes whose pitch depends on where you stand, so every spot sounds a little different. In
 * the Shadow City there are doors, steps and whispers around you, but nobody to make them.
 */
public final class SculkAmbience {
	private static final ResourceKey<Biome> PILLARS = ResourceKey.create(Registries.BIOME, EndRealms.id("listening_pillars"));
	private static final ResourceKey<Biome> CITY = ResourceKey.create(Registries.BIOME, EndRealms.id("shadow_city"));
	/** A minor scale, low: the organ's notes. */
	private static final float[] NOTES = {0.5F, 0.56F, 0.59F, 0.67F, 0.75F, 0.79F, 0.89F, 1.0F};

	private SculkAmbience() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(SculkAmbience::tick);
	}

	private static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null || minecraft.isPaused() || level.dimension() != SculkDimension.LEVEL) {
			return;
		}
		BlockPos at = minecraft.player.blockPosition();
		RandomSource random = level.getRandom();
		if (level.getBiome(at).is(PILLARS) && level.getGameTime() % 50 == 0) {
			int chord = Math.floorMod(at.getX() / 6 * 7 + at.getZ() / 6 * 13, NOTES.length);
			for (int i = 0; i < 2; i++) {
				float pitch = NOTES[(chord + i * 2) % NOTES.length];
				level.playLocalSound(minecraft.player.getX() + random.nextGaussian() * 8, minecraft.player.getY() + 6 + random.nextInt(10),
						minecraft.player.getZ() + random.nextGaussian() * 8, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.AMBIENT,
						0.35F, pitch, false);
			}
		}
		if (level.getBiome(at).is(CITY) && random.nextInt(260) == 0) {
			SoundEvent[] sounds = {SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.SOUL_ESCAPE.value(),
					SoundEvents.STONE_STEP, SoundEvents.CHEST_CLOSE, SoundEvents.AMBIENT_CAVE.value()};
			SoundEvent sound = sounds[random.nextInt(sounds.length)];
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 6 + random.nextDouble() * 10;
			level.playLocalSound(minecraft.player.getX() + Math.cos(angle) * distance, minecraft.player.getY() + random.nextInt(3),
					minecraft.player.getZ() + Math.sin(angle) * distance, sound, SoundSource.AMBIENT, 0.6F, 0.6F + random.nextFloat() * 0.3F, false);
		}
	}
}
