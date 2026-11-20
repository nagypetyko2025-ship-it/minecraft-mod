package hu.endrealms.admin;

import dev.wroud.mc.worlds.McWorld;
import dev.wroud.mc.worlds.manager.group.WorldGroup;
import dev.wroud.mc.worlds.manager.group.WorldKind;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.storage.LevelData;

/**
 * Everything that talks to the Worlds mod (Wroud, MIT). Only touched when that mod is loaded, so the server runs without it.
 * A world made from the panel is a Worlds group: an overworld with its own Nether and End, linked by portals.
 */
final class MultiWorlds {
	private MultiWorlds() {
	}

	/** One entry per group, then one per world outside any group; each lists its member dimensions, overworld first. */
	static List<Entry> entries(MinecraftServer server) {
		McWorld worlds = McWorld.of(server);
		List<Entry> list = new ArrayList<>();
		for (WorldGroup group : worlds.groups().values()) {
			List<Identifier> members = new ArrayList<>();
			for (WorldKind kind : new WorldKind[] {WorldKind.OVERWORLD, WorldKind.NETHER, WorldKind.END}) {
				Identifier main = group.main(kind);
				if (main != null) {
					members.add(main);
				}
			}
			for (Identifier member : group.members()) {
				if (!members.contains(member)) {
					members.add(member);
				}
			}
			list.add(new Entry(group.id(), true, members, group.main(WorldKind.OVERWORLD)));
		}
		for (Identifier world : worlds.worlds()) {
			if (world.getNamespace().equals("minecraft") && (world.getPath().equals("overworld") || world.getPath().equals("the_nether") || world.getPath().equals("the_end"))) {
				continue;     // the main world's own dimensions
			}
			if (worlds.groupOf(world).equals(McWorld.DEFAULT_GROUP)) {
				list.add(new Entry(world, false, List.of(world), world));
			}
		}
		return list;
	}

	record Entry(Identifier id, boolean group, List<Identifier> members, Identifier overworld) {
	}

	static boolean exists(MinecraftServer server, Identifier id) {
		McWorld worlds = McWorld.of(server);
		return worlds.worlds().contains(id) || worlds.group(id) != null;
	}

	static boolean isLoaded(MinecraftServer server, Identifier world) {
		return McWorld.of(server).isLoaded(world);
	}

	static LevelData.RespawnData spawn(MinecraftServer server, Identifier world) {
		return McWorld.of(server).worldSpawn(world);
	}

	/** "Overworld", "Nether" or "End", from the group's own slots; anything else is just a member. */
	static String kind(MinecraftServer server, Identifier group, Identifier world) {
		WorldGroup g = McWorld.of(server).group(group);
		if (g != null) {
			for (WorldKind kind : new WorldKind[] {WorldKind.OVERWORLD, WorldKind.NETHER, WorldKind.END}) {
				if (world.equals(g.main(kind))) {
					return kind.name().toLowerCase(java.util.Locale.ROOT);
				}
			}
		}
		return "other";
	}

	/**
	 * Makes the overworld (and if asked its Nether and End), then one group holding them, so portals lead between them and
	 * players keep a separate inventory there. Calls back on the server thread with null or what went wrong.
	 */
	static void create(MinecraftServer server, Identifier id, ResourceKey<WorldPreset> preset, Long seed, boolean netherAndEnd, Consumer<Throwable> done) {
		McWorld worlds = McWorld.of(server);
		Identifier nether = id.withSuffix("_nether");
		Identifier end = id.withSuffix("_end");
		List<CompletableFuture<ServerLevel>> futures = new ArrayList<>();
		try {
			futures.add(worlds.createWorld(id, worlds.levelStem(preset, null), seed));
			if (netherAndEnd) {
				futures.add(worlds.createWorld(nether, worlds.levelStem(LevelStem.NETHER), seed));
				futures.add(worlds.createWorld(end, worlds.levelStem(LevelStem.END), seed));
			}
		} catch (RuntimeException exception) {
			done.accept(exception);
			return;
		}
		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((ignored, failure) -> server.execute(() -> {
			if (failure != null) {
				done.accept(failure);
				return;
			}
			try {
				WorldGroup group = worlds.createGroup(id);
				group.join(id, true);
				if (netherAndEnd) {
					group.join(nether, true);
					group.join(end, true);
				}
				done.accept(null);
			} catch (RuntimeException exception) {
				done.accept(exception);
			}
		}));
	}

	static boolean teleport(MinecraftServer server, ServerPlayer player, Identifier world) {
		return McWorld.of(server).teleport(player, world);
	}

	// ---------------------------------------------------------------- switching worlds

	/** Players who just changed worlds, and the group they went to; settled on the next tick. */
	private static final Map<UUID, Identifier> ARRIVED = new ConcurrentHashMap<>();

	/**
	 * A world switch builds a brand-new player object from that world's saved copy. Two things are fixed up here:
	 * the login (EasyAuth's state lives on the player object, so without this everyone would have to log in again in
	 * every world), and where they land: at their own spawn point in that world (bed or anchor) when they have one.
	 */
	static void register() {
		McWorld.AFTER_SWITCH.register((oldPlayer, newPlayer, from, to) -> {
			keepLogin(oldPlayer, newPlayer);
			ARRIVED.put(newPlayer.getUUID(), to);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (ARRIVED.isEmpty()) {
				return;
			}
			Map<UUID, Identifier> arrived = new HashMap<>(ARRIVED);
			ARRIVED.clear();
			arrived.forEach((id, group) -> {
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player != null) {
					toSpawnPoint(server, player, group);
				}
			});
		});
	}

	private static void toSpawnPoint(MinecraftServer server, ServerPlayer player, Identifier group) {
		ServerPlayer.RespawnConfig config = player.getRespawnConfig();
		if (config == null) {
			return;     // never slept there: the world's own spawn, where Worlds already put them
		}
		ResourceKey<Level> dimension = config.respawnData().dimension();
		if (!McWorld.of(server).groupOf(dimension.identifier()).equals(group) || server.getLevel(dimension) == null) {
			return;
		}
		TeleportTransition target = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
		if (!target.missingRespawnBlock() && target.newLevel().dimension().equals(dimension)) {
			player.teleport(target);
		}
	}

	/** Copies EasyAuth's login onto the new player object; found by name, so nothing breaks without EasyAuth. */
	private static void keepLogin(ServerPlayer from, ServerPlayer to) {
		try {
			Class<?> type = from.getClass();
			if ((boolean) type.getMethod("easyAuth$isAuthenticated").invoke(from)) {
				Method getEntry = type.getMethod("easyAuth$getPlayerEntryV1");
				Object entry = getEntry.invoke(from);
				if (entry != null) {
					to.getClass().getMethod("easyAuth$setPlayerEntryV1", getEntry.getReturnType()).invoke(to, entry);
				}
				Object ip = type.getMethod("easyAuth$getIpAddress").invoke(from);
				if (ip != null) {
					to.getClass().getMethod("easyAuth$setIpAddress", String.class).invoke(to, ip);
				}
				if ((boolean) type.getMethod("easyAuth$isUsingMojangAccount").invoke(from)) {
					to.getClass().getMethod("easyAuth$setUsingMojangAccount").invoke(to);
				}
				if ((boolean) type.getMethod("easyAuth$canSkipAuth").invoke(from)) {
					to.getClass().getMethod("easyAuth$setSkipAuth").invoke(to);
				}
				to.getClass().getMethod("easyAuth$setAuthenticated", boolean.class).invoke(to, true);
			}
		} catch (NoSuchMethodException exception) {
			// no EasyAuth on this server
		} catch (ReflectiveOperationException | RuntimeException exception) {
			hu.endrealms.EndRealms.LOGGER.warn("Could not carry the login over to {}'s new world", from.getGameProfile().name(), exception);
		}
	}
}
