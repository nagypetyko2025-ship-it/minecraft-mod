package hu.endrealms.entity;

import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

/** Keeps a few Spectral Whelps in the air over the Dragon Graveyard around each player; see {@link AshWraithSpawner} for why. */
public final class SpectralWhelpSpawner {
	private static final int INTERVAL_TICKS = 120;
	private static final int MAX_NEARBY = 3;
	private static final double COUNT_RANGE = 56.0;
	private static final int MIN_DISTANCE = 16;
	private static final int MAX_DISTANCE = 36;
	private static final int ATTEMPTS = 8;

	private SpectralWhelpSpawner() {
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL_TICKS != 0) {
			return;
		}
		ServerLevel level = server.getLevel(Level.END);
		if (level == null || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && level.getEntitiesOfClass(SpectralWhelp.class, player.getBoundingBox().inflate(COUNT_RANGE)).size() < MAX_NEARBY) {
				spawnNear(level, player);
			}
		}
	}

	private static void spawnNear(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
			int x = player.getBlockX() + Math.round(Mth.cos(angle) * distance);
			int z = player.getBlockZ() + Math.round(Mth.sin(angle) * distance);
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
			if (top.getY() <= level.getMinY() + 4 || !level.getBiome(top).is(ModWorldgen.DRAGON_GRAVEYARD)) {
				continue;
			}
			BlockPos air = top.above(8 + random.nextInt(7));
			SpectralWhelp whelp = ModEntities.SPECTRAL_WHELP.create(level, EntitySpawnReason.NATURAL);
			if (whelp == null) {
				return;
			}
			whelp.snapTo(air.getX() + 0.5, air.getY(), air.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			if (level.noCollision(whelp)) {
				whelp.finalizeSpawn(level, level.getCurrentDifficultyAt(air), EntitySpawnReason.NATURAL, null);
				level.addFreshEntity(whelp);
				return;
			}
		}
	}
}
