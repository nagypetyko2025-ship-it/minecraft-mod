package hu.endrealms.entity;

import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModWorldgen;
import hu.endrealms.worldgen.AstralBasinFeature;
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

/** Keeps Deep Lurkers in the deep water around each player; like the Ash Wraiths, the vanilla spawner cannot do it in the End. */
public final class DeepLurkerSpawner {
	private static final int INTERVAL_TICKS = 200;
	private static final int MAX_NEARBY = 2;
	private static final double COUNT_RANGE = 48.0;
	private static final int MIN_DISTANCE = 12;
	private static final int MAX_DISTANCE = 32;
	private static final int MIN_WATER_DEPTH = 6;
	private static final int ATTEMPTS = 10;

	private DeepLurkerSpawner() {
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
			if (!player.isSpectator() && level.getEntitiesOfClass(DeepLurker.class, player.getBoundingBox().inflate(COUNT_RANGE)).size() < MAX_NEARBY) {
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
			int bed = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
			int top = AstralBasinFeature.WATER_LEVEL - 2;
			if (top - bed < MIN_WATER_DEPTH) {
				continue;
			}
			BlockPos pos = new BlockPos(x, bed + 1 + random.nextInt(top - bed), z);
			if (!level.getBiome(pos).is(ModWorldgen.ASTRAL_BASIN) || level.getFluidState(pos).isEmpty() || level.getFluidState(pos.above()).isEmpty()) {
				continue;
			}
			DeepLurker lurker = ModEntities.DEEP_LURKER.create(level, EntitySpawnReason.NATURAL);
			if (lurker == null) {
				return;
			}
			lurker.snapTo(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			if (level.noCollision(lurker)) {
				lurker.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
				level.addFreshEntity(lurker);
				return;
			}
		}
	}
}
