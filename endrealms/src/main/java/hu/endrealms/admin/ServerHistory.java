package hu.endrealms.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.endrealms.EndRealms;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

/**
 * history.json in the server folder: every day the server ran, with who joined that day. The control panel's calendar
 * reads it; unlike the game's logs, which drop older files on days with many restarts, nothing is ever thrown away.
 */
final class ServerHistory {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int CHECK_TICKS = 20 * 60;

	private final Map<String, TreeSet<String>> days = new TreeMap<>();
	/** Player UUID -> last known name: the game's own name cache forgets players after a month. */
	private final Map<String, String> names = new TreeMap<>();
	private Path file;

	void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(this::start);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			String name = handler.player.getName().getString();
			synchronized (this) {
				if (!name.equals(names.put(handler.player.getUUID().toString(), name)) && file != null) {
					save();
				}
			}
			mark(name);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_TICKS == 0) {
				mark(null);                // a server running past midnight counts for the new day too
			}
		});
	}

	private void start(MinecraftServer server) {
		file = server.getServerDirectory().resolve("history.json");
		try {
			if (Files.exists(file)) {
				JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
				for (Map.Entry<String, com.google.gson.JsonElement> entry : root.getAsJsonObject("days").entrySet()) {
					TreeSet<String> players = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
					entry.getValue().getAsJsonArray().forEach(name -> players.add(name.getAsString()));
					days.put(entry.getKey(), players);
				}
				if (root.has("names")) {
					root.getAsJsonObject("names").entrySet().forEach(entry -> names.put(entry.getKey(), entry.getValue().getAsString()));
				}
			}
		} catch (Exception exception) {
			EndRealms.LOGGER.warn("Could not read history.json, starting a new one", exception);
		}
		mark(null);
	}

	private synchronized void mark(String player) {
		if (file == null) {
			return;
		}
		String today = LocalDate.now().toString();
		TreeSet<String> players = days.get(today);
		boolean changed = players == null;
		if (players == null) {
			players = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
			days.put(today, players);
		}
		if (player != null) {
			changed |= players.add(player);
		}
		if (changed) {
			save();
		}
	}

	synchronized Map<String, String> names() {
		return new TreeMap<>(names);
	}

	/** The days the given player joined, oldest first. */
	synchronized List<String> daysOf(String player) {
		List<String> list = new ArrayList<>();
		days.forEach((day, players) -> {
			if (players.contains(player)) {
				list.add(day);
			}
		});
		return list;
	}

	private void save() {
		JsonObject root = new JsonObject();
		JsonObject out = new JsonObject();
		days.forEach((day, players) -> {
			JsonArray names = new JsonArray();
			players.forEach(names::add);
			out.add(day, names);
		});
		root.add("days", out);
		JsonObject known = new JsonObject();
		names.forEach(known::addProperty);
		root.add("names", known);
		try {
			Path temp = file.resolveSibling("history.json.tmp");
			Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
			Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException exception) {
			EndRealms.LOGGER.warn("Could not write history.json", exception);
		}
	}
}
