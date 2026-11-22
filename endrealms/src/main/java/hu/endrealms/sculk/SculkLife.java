package hu.endrealms.sculk;

import hu.endrealms.registry.ModBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What the Sculk dimension's plants do around players: the Echo Flower plays your footsteps back to you a little
 * later, from where you walked; the eyes on the Watcher Grass turn after you, and shut while you sneak.
 */
public final class SculkLife {
	private static final int CHECK_EVERY = 10;
	private static final int ECHO_RANGE = 5;
	private static final int ECHO_DELAY = 50;
	private static final int ECHO_COOLDOWN = 200;
	private static final int EYES_RANGE = 9;

	/** A footstep waiting to be played back. */
	private record Echo(ServerLevel level, Vec3 where, BlockState ground, long when) {
	}

	private static final List<Echo> ECHOES = new ArrayList<>();
	private static final Map<UUID, Long> ECHO_READY = new HashMap<>();
	private static final Map<UUID, Vec3> LAST = new HashMap<>();

	private SculkLife() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(SculkLife::tick);
	}

	private static void tick(MinecraftServer server) {
		ServerLevel sculk = server.getLevel(SculkDimension.LEVEL);
		long now = server.getTickCount();
		for (Iterator<Echo> it = ECHOES.iterator(); it.hasNext(); ) {
			Echo echo = it.next();
			if (now >= echo.when) {
				SoundType sound = echo.ground.getSoundType();
				echo.level.playSound(null, echo.where.x, echo.where.y, echo.where.z, sound.getStepSound(), SoundSource.AMBIENT,
						sound.getVolume() * 0.6F, sound.getPitch() * 0.85F);
				it.remove();
			}
		}
		if (sculk == null || now % CHECK_EVERY != 0) {
			return;
		}
		for (ServerPlayer player : sculk.players()) {
			if (player.isSpectator()) {
				continue;
			}
			echo(sculk, player, now);
			if (now % (CHECK_EVERY * 2) == 0) {
				watch(sculk, player);
			}
		}
	}

	/** Near an Echo Flower, the last few steps you took are heard again behind you, as if someone followed. */
	private static void echo(ServerLevel level, ServerPlayer player, long now) {
		Vec3 here = player.position();
		Vec3 before = LAST.put(player.getUUID(), here);
		if (before == null || before.distanceToSqr(here) < 0.5 || player.isShiftKeyDown() || !player.onGround()
				|| now < ECHO_READY.getOrDefault(player.getUUID(), 0L)) {
			return;
		}
		BlockPos center = player.blockPosition();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -ECHO_RANGE; dx <= ECHO_RANGE; dx++) {
			for (int dz = -ECHO_RANGE; dz <= ECHO_RANGE; dz++) {
				for (int dy = -2; dy <= 2; dy++) {
					if (level.getBlockState(pos.setWithOffset(center, dx, dy, dz)).is(ModBlocks.ECHO_FLOWER)) {
						BlockState ground = level.getBlockState(BlockPos.containing(before).below());
						for (int step = 0; step < 4; step++) {
							ECHOES.add(new Echo(level, before, ground, now + ECHO_DELAY + step * 7L));
						}
						ECHO_READY.put(player.getUUID(), now + ECHO_COOLDOWN);
						return;
					}
				}
			}
		}
	}

	/** The Watcher Grass around a player looks at them; sneaking makes them shut their eyes. */
	private static void watch(ServerLevel level, ServerPlayer player) {
		BlockPos center = player.blockPosition();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean shut = player.isShiftKeyDown();
		for (int dx = -EYES_RANGE; dx <= EYES_RANGE; dx++) {
			for (int dz = -EYES_RANGE; dz <= EYES_RANGE; dz++) {
				for (int dy = -3; dy <= 5; dy++) {
					BlockState state = level.getBlockState(pos.setWithOffset(center, dx, dy, dz));
					if (state.is(ModBlocks.WATCHER_GRASS) && state.getValue(SculkPlants.WatcherGrass.PART) == SculkPlants.Part.TOP) {
						SculkPlants.look(level, pos.immutable(), state, player.getX(), player.getZ(), shut);
					}
				}
			}
		}
	}
}
