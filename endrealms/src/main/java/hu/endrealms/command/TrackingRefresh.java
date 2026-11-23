package hu.endrealms.command;

import hu.endrealms.EndRealms;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * A fix for players turning invisible to each other after a teleport within the same dimension: the one who arrives
 * can be seen and hit by the other, but does not see them, until relogging. The server's entity tracking (reworked
 * by the VMP performance mod) misses the pair after the jump.
 * <p>
 * So whenever a player jumps more than {@link #JUMP} blocks in one tick - /tp, /tpa, a pearl, a rift blade - the
 * tracking between them and every player around the place they landed is redone, twice: a moment after the jump and
 * again two seconds later, once the chunks there have surely reached them. Redoing it means telling each client to
 * forget the other player and then sending the player again, which is what a relog does.
 */
public final class TrackingRefresh {
	private static final double JUMP = 32.0;
	private static final double AROUND = 160.0;
	private static final int[] DELAYS = {5, 40};

	private record Pending(UUID player, long when) {
	}

	private static final Map<UUID, Vec3> LAST = new HashMap<>();
	private static final List<Pending> PENDING = new ArrayList<>();
	private static Field entityMap;
	private static Method removePlayer;
	private static Method updatePlayer;
	private static boolean broken;

	private TrackingRefresh() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(TrackingRefresh::tick);
	}

	private static void tick(MinecraftServer server) {
		long now = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Vec3 here = player.position();
			Vec3 before = LAST.put(player.getUUID(), here);
			if (before != null && before.distanceToSqr(here) > JUMP * JUMP) {
				for (int delay : DELAYS) {
					PENDING.add(new Pending(player.getUUID(), now + delay));
				}
			}
		}
		LAST.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
		for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
			Pending pending = it.next();
			if (now < pending.when) {
				continue;
			}
			it.remove();
			ServerPlayer player = server.getPlayerList().getPlayer(pending.player);
			if (player != null) {
				refresh(player);
			}
		}
	}

	/** Makes {@code player} and every player near them see each other afresh. */
	public static void refresh(ServerPlayer player) {
		if (broken) {
			return;
		}
		ServerLevel level = player.level();
		for (ServerPlayer other : level.players()) {
			if (other != player && other.distanceToSqr(player) < AROUND * AROUND) {
				retrack(level, other, player);
				retrack(level, player, other);
			}
		}
	}

	/** Has {@code viewer} forget {@code seen} and track it again, as on a fresh login. */
	@SuppressWarnings("unchecked")
	private static void retrack(ServerLevel level, ServerPlayer seen, ServerPlayer viewer) {
		try {
			if (entityMap == null) {
				entityMap = ChunkMap.class.getDeclaredField("entityMap");
				entityMap.setAccessible(true);
				Class<?> tracked = Class.forName("net.minecraft.server.level.ChunkMap$TrackedEntity");
				removePlayer = tracked.getDeclaredMethod("removePlayer", ServerPlayer.class);
				updatePlayer = tracked.getDeclaredMethod("updatePlayer", ServerPlayer.class);
				removePlayer.setAccessible(true);
				updatePlayer.setAccessible(true);
			}
			Object entry = ((Int2ObjectMap<Object>) entityMap.get(level.getChunkSource().chunkMap)).get(seen.getId());
			if (entry != null) {
				removePlayer.invoke(entry, viewer);
				updatePlayer.invoke(entry, viewer);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			broken = true;
			EndRealms.LOGGER.warn("Could not refresh player tracking after a teleport; leaving it to the game", e);
		}
	}
}
