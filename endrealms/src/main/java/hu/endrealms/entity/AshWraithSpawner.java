package hu.endrealms.entity;

import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Keeps Ash Wraiths on the surface of the Scorched Barrens around each player. The vanilla spawner cannot do this: the
 * End's monster cap is permanently full of endermen, and it picks a random height, which mostly misses a hilly surface.
 */
public final class AshWraithSpawner {
	private static final int INTERVAL_TICKS = 100;
	private static final int MAX_NEARBY = 4;
	private static final double COUNT_RANGE = 48.0;
	private static final int MIN_DISTANCE = 18;
	private static final int MAX_DISTANCE = 38;
	private static final int ATTEMPTS = 10;

	private AshWraithSpawner() {
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
			if (!player.isSpectator() && level.getEntitiesOfClass(AshWraith.class, player.getBoundingBox().inflate(COUNT_RANGE)).size() < MAX_NEARBY) {
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
			if (!level.getBiome(top).is(ModWorldgen.SCORCHED_BARRENS)
					|| !level.getBlockState(top.below()).isFaceSturdy(level, top.below(), Direction.UP)) {
				continue;
			}
			VoxelShape ash = level.getBlockState(top).getCollisionShape(level, top);
			double y = top.getY() + (ash.isEmpty() ? 0.0 : ash.max(Direction.Axis.Y));
			AshWraith wraith = ModEntities.ASH_WRAITH.create(level, EntitySpawnReason.NATURAL);
			if (wraith == null) {
				return;
			}
			wraith.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			if (level.noCollision(wraith)) {
				wraith.finalizeSpawn(level, level.getCurrentDifficultyAt(top), EntitySpawnReason.NATURAL, null);
				level.addFreshEntity(wraith);
				return;
			}
		}
	}
}
