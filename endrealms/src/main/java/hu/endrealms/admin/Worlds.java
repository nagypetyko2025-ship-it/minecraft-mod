package hu.endrealms.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.endrealms.EndRealms;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The control panel's "Világok" tab: the server's main world, plus the worlds made with the Worlds mod when the server has
 * it. Names given in the panel, and what we know about worlds made here (seed, type, when), live in worlds.json.
 */
final class Worlds {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final long SIZE_CACHE_MS = 120_000L;
	private static final int MAX_NAME = 40;
	private static final String NAMESPACE = "vilag";
	private static final Map<String, ResourceKey<WorldPreset>> TYPES = Map.of(
			"normal", WorldPresets.NORMAL, "flat", WorldPresets.FLAT, "amplified", WorldPresets.AMPLIFIED, "large_biomes", WorldPresets.LARGE_BIOMES);

	private final Map<Path, long[]> sizes = new ConcurrentHashMap<>();     // folder -> {bytes, measured at}
	/** Worlds being made right now, or that failed: id -> null while working, else the error. */
	private final Map<String, String> pending = new ConcurrentHashMap<>();

	static boolean available() {
		return FabricLoader.getInstance().isModLoaded("mc-worlds");
	}

	// ---------------------------------------------------------------- worlds.json

	private Path metaFile(MinecraftServer server) {
		return server.getServerDirectory().resolve("worlds.json");
	}

	private JsonObject meta(MinecraftServer server) {
		try {
			return JsonParser.parseString(Files.readString(metaFile(server), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception exception) {
			return new JsonObject();
		}
	}

	private JsonObject metaOf(MinecraftServer server, String id) {
		JsonObject all = meta(server);
		return all.has(id) ? all.getAsJsonObject(id) : new JsonObject();
	}

	private synchronized void saveMeta(MinecraftServer server, String id, JsonObject entry) {
		JsonObject all = meta(server);
		if (entry == null || entry.size() == 0) {
			all.remove(id);
		} else {
			all.add(id, entry);
		}
		try {
			Path file = metaFile(server);
			Path temp = file.resolveSibling("worlds.json.tmp");
			Files.writeString(temp, GSON.toJson(all), StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not write worlds.json", exception);
		}
	}

	private String nameOf(MinecraftServer server, String id, String fallback) {
		JsonObject entry = metaOf(server, id);
		return entry.has("name") ? entry.get("name").getAsString() : fallback;
	}

	// ---------------------------------------------------------------- where things are

	private Path root(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
	}

	private String mainId(MinecraftServer server) {
		return root(server).getFileName().toString();
	}

	private static Path dimensionFolder(Path root, Identifier key) {
		return root.resolve("dimensions").resolve(key.getNamespace()).resolve(key.getPath());
	}

	private static ServerLevel level(MinecraftServer server, Identifier id) {
		return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	private static boolean vanilla(Identifier dimension) {
		return dimension.equals(Level.OVERWORLD.identifier()) || dimension.equals(Level.NETHER.identifier()) || dimension.equals(Level.END.identifier());
	}

	/** The main world's dimensions: the vanilla three and any other dimension a mod adds that is not a Worlds world. */
	private List<Identifier> mainDimensions(MinecraftServer server, List<MultiWorlds.Entry> extra) {
		List<Identifier> taken = new ArrayList<>();
		extra.forEach(e -> taken.addAll(e.members()));
		List<Identifier> list = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			Identifier id = level.dimension().identifier();
			if (vanilla(id) || !taken.contains(id)) {
				list.add(id);
			}
		}
		return list;
	}

	private List<MultiWorlds.Entry> extra(MinecraftServer server) {
		return available() ? MultiWorlds.entries(server) : List.of();
	}

	/** On the server thread: the folders whose sizes the panel shows. */
	List<Path> folders(MinecraftServer server) {
		Path root = root(server);
		List<Path> folders = new ArrayList<>();
		folders.add(root);
		List<MultiWorlds.Entry> extra = extra(server);
		for (Identifier dimension : mainDimensions(server, extra)) {
			folders.add(dimensionFolder(root, dimension));
		}
		for (MultiWorlds.Entry entry : extra) {
			for (Identifier member : entry.members()) {
				folders.add(dimensionFolder(root, member));
			}
		}
		return folders;
	}

	/** Off the server thread: walks the folders whose size is unknown or stale. */
	void measure(List<Path> folders) {
		long now = System.currentTimeMillis();
		for (Path folder : folders) {
			long[] known = sizes.get(folder);
			if (known == null || now - known[1] > SIZE_CACHE_MS) {
				sizes.put(folder, new long[] {size(folder), now});
			}
		}
	}

	private long cachedSize(Path folder) {
		long[] known = sizes.get(folder);
		return known == null ? -1L : known[0];
	}

	private static long size(Path folder) {
		if (!Files.isDirectory(folder)) {
			return 0L;
		}
		try (Stream<Path> files = Files.walk(folder)) {
			return files.filter(Files::isRegularFile).mapToLong(file -> {
				try {
					return Files.size(file);
				} catch (IOException exception) {
					return 0L;
				}
			}).sum();
		} catch (IOException | RuntimeException exception) {
			return -1L;
		}
	}

	/** The main world's size without the Worlds worlds, which live inside its folder. */
	private long mainSize(MinecraftServer server, List<MultiWorlds.Entry> extra) {
		long total = cachedSize(root(server));
		if (total < 0) {
			return total;
		}
		for (MultiWorlds.Entry entry : extra) {
			for (Identifier member : entry.members()) {
				total -= Math.max(0L, cachedSize(dimensionFolder(root(server), member)));
			}
		}
		return total;
	}

	private long membersSize(MinecraftServer server, List<Identifier> members) {
		long total = 0;
		for (Identifier member : members) {
			long size = cachedSize(dimensionFolder(root(server), member));
			if (size < 0) {
				return -1L;
			}
			total += size;
		}
		return total;
	}

	private static int playersIn(MinecraftServer server, List<Identifier> dimensions) {
		int count = 0;
		for (Identifier dimension : dimensions) {
			ServerLevel level = level(server, dimension);
			count += level == null ? 0 : level.players().size();
		}
		return count;
	}

	// ---------------------------------------------------------------- the list

	/** Runs on the server thread. */
	Map<String, Object> list(MinecraftServer server) {
		List<MultiWorlds.Entry> extra = extra(server);
		List<Map<String, Object>> worlds = new ArrayList<>();
		String mainId = mainId(server);
		List<Identifier> mainDims = mainDimensions(server, extra);
		Map<String, Object> main = new LinkedHashMap<>();
		main.put("id", mainId);
		main.put("name", nameOf(server, mainId, server.getWorldData().getLevelName()));
		main.put("main", true);
		main.put("online", playersIn(server, mainDims));
		main.put("dimensions", mainDims.size());
		main.put("day", server.overworld().getOverworldClockTime() / 24000L);
		main.put("loaded", true);
		main.put("sizeBytes", mainSize(server, extra));
		worlds.add(main);
		for (MultiWorlds.Entry entry : extra) {
			String id = entry.id().toString();
			Map<String, Object> w = new LinkedHashMap<>();
			w.put("id", id);
			w.put("name", nameOf(server, id, entry.id().getPath()));
			w.put("main", false);
			w.put("online", playersIn(server, entry.members()));
			w.put("dimensions", entry.members().size());
			ServerLevel overworld = entry.overworld() == null ? null : level(server, entry.overworld());
			w.put("day", overworld == null ? null : overworld.getDefaultClockTime() / 24000L);
			w.put("loaded", overworld != null);
			w.put("sizeBytes", membersSize(server, entry.members()));
			worlds.add(w);
		}
		pending.forEach((id, error) -> {
			if (extra.stream().noneMatch(e -> e.id().toString().equals(id)) || error != null) {
				Map<String, Object> w = new LinkedHashMap<>();
				w.put("id", id);
				w.put("name", nameOf(server, id, id));
				w.put("main", false);
				w.put("creating", error == null || error.isEmpty());
				w.put("error", error == null || error.isEmpty() ? null : error);
				worlds.removeIf(existing -> existing.get("id").equals(id));
				worlds.add(w);
			}
		});
		Map<String, Object> answer = new LinkedHashMap<>();
		answer.put("multiworld", available());
		answer.put("worlds", worlds);
		return answer;
	}

	// ---------------------------------------------------------------- one world

	/** Runs on the server thread; sizes come from the cache, {@link #measure} fills it off the server thread. */
	Map<String, Object> describe(MinecraftServer server, String id) {
		List<MultiWorlds.Entry> extra = extra(server);
		Map<String, Object> d = new LinkedHashMap<>();
		List<Identifier> dimensions;
		ServerLevel overworld;
		Path root = root(server);
		if (mainId(server).equals(id)) {
			dimensions = mainDimensions(server, extra);
			overworld = server.overworld();
			d.put("name", nameOf(server, id, server.getWorldData().getLevelName()));
			d.put("main", true);
			d.put("levelName", server.getWorldData().getLevelName());
			d.put("folder", root.toString());
			try {
				d.put("realFolder", root.toRealPath().toString());
			} catch (IOException exception) {
				d.put("realFolder", root.toString());
			}
			d.put("seed", String.valueOf(overworld.getSeed()));
			d.put("day", overworld.getOverworldClockTime() / 24000L);
			LevelData.RespawnData spawn = overworld.getRespawnData();
			d.put("spawn", spawnMap(spawn));
			d.put("firstDay", firstDay(server));
			d.put("knownPlayers", count(server.getWorldPath(LevelResource.PLAYER_DATA_DIR), ".dat"));
			d.put("sizeBytes", mainSize(server, extra));
			d.put("travelTo", Level.OVERWORLD.identifier().toString());
		} else {
			MultiWorlds.Entry entry = extra.stream().filter(e -> e.id().toString().equals(id)).findFirst().orElse(null);
			if (entry == null) {
				return null;
			}
			dimensions = entry.members();
			overworld = entry.overworld() == null ? null : level(server, entry.overworld());
			JsonObject meta = metaOf(server, id);
			d.put("name", nameOf(server, id, entry.id().getPath()));
			d.put("main", false);
			d.put("worldId", id);
			d.put("folder", dimensionFolder(root, entry.overworld() == null ? entry.id() : entry.overworld()).toString());
			d.put("seed", overworld != null ? String.valueOf(overworld.getSeed()) : meta.has("seed") ? meta.get("seed").getAsString() : null);
			d.put("type", meta.has("type") ? meta.get("type").getAsString() : null);
			d.put("created", meta.has("created") ? meta.get("created").getAsLong() : null);
			d.put("day", overworld == null ? null : overworld.getDefaultClockTime() / 24000L);
			LevelData.RespawnData spawn = entry.overworld() == null ? null : MultiWorlds.spawn(server, entry.overworld());
			d.put("spawn", spawn == null ? null : spawnMap(spawn));
			d.put("sizeBytes", membersSize(server, entry.members()));
			d.put("travelTo", entry.overworld() == null ? null : entry.overworld().toString());
			d.put("group", entry.group());
		}
		d.put("id", id);
		d.put("loaded", overworld != null);
		d.put("multiworld", available());
		d.put("gameVersion", SharedConstants.getCurrentVersion().name());
		d.put("modVersion", FabricLoader.getInstance().getModContainer(EndRealms.MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?"));
		d.put("hardcore", server.isHardcore());
		d.put("difficulty", server.getWorldData().getDifficulty().getSerializedName());
		d.put("gameMode", server.getDefaultGameType().getName());
		if (overworld != null) {
			d.put("gameTicks", overworld.getGameTime());
			d.put("border", Math.round(overworld.getWorldBorder().getSize()));
			d.put("weather", overworld.isThundering() ? "thunder" : overworld.isRaining() ? "rain" : "clear");
		}
		try {
			d.put("lastSave", Files.getLastModifiedTime(server.getWorldPath(LevelResource.LEVEL_DATA_FILE)).toMillis());
		} catch (IOException exception) {
			d.put("lastSave", 0L);
		}

		List<Map<String, Object>> dims = new ArrayList<>();
		for (Identifier key : dimensions) {
			Map<String, Object> dim = new LinkedHashMap<>();
			dim.put("id", key.toString());
			dim.put("kind", kindOf(server, id, key));
			ServerLevel level = level(server, key);
			dim.put("loaded", level != null);
			if (level != null) {
				dim.put("loadedChunks", level.getChunkSource().getLoadedChunksCount());
				int entities = 0;
				for (Entity ignored : level.getAllEntities()) {
					entities++;
				}
				dim.put("entities", entities);
				List<String> players = new ArrayList<>();
				for (ServerPlayer player : level.players()) {
					players.add(player.getGameProfile().name());
				}
				dim.put("players", players);
			}
			dim.put("sizeBytes", cachedSize(dimensionFolder(root, key)));
			dims.add(dim);
		}
		d.put("dimensions", dims);
		List<String> online = new ArrayList<>();
		server.getPlayerList().getPlayers().forEach(p -> online.add(p.getGameProfile().name()));
		d.put("onlinePlayers", online);
		return d;
	}

	/** For the "who is playing" table: the panel name of the world a dimension belongs to, and which of its dimensions it is. Server thread. */
	Map<String, String> whereIs(MinecraftServer server, Identifier dimension) {
		for (MultiWorlds.Entry entry : extra(server)) {
			if (entry.members().contains(dimension)) {
				String id = entry.id().toString();
				return Map.of("world", nameOf(server, id, entry.id().getPath()), "worldId", id, "kind", kindOf(server, id, dimension));
			}
		}
		String id = mainId(server);
		return Map.of("world", nameOf(server, id, server.getWorldData().getLevelName()), "worldId", id, "kind", kindOf(server, id, dimension));
	}

	private String kindOf(MinecraftServer server, String worldId, Identifier dimension) {
		if (dimension.equals(Level.OVERWORLD.identifier())) {
			return "overworld";
		}
		if (dimension.equals(Level.NETHER.identifier())) {
			return "nether";
		}
		if (dimension.equals(Level.END.identifier())) {
			return "end";
		}
		Identifier group = Identifier.tryParse(worldId);
		return available() && group != null ? MultiWorlds.kind(server, group, dimension) : "other";
	}

	private static Map<String, Object> spawnMap(LevelData.RespawnData spawn) {
		return Map.of("dimension", spawn.dimension().identifier().toString(), "x", spawn.pos().getX(), "y", spawn.pos().getY(), "z", spawn.pos().getZ());
	}

	private static int count(Path folder, String suffix) {
		try (Stream<Path> files = Files.list(folder)) {
			return (int) files.filter(file -> file.getFileName().toString().endsWith(suffix)).count();
		} catch (IOException exception) {
			return 0;
		}
	}

	/** The first day the server ran: the oldest archived log or the oldest day of history.json. */
	private static String firstDay(MinecraftServer server) {
		String first = null;
		try (Stream<Path> logs = Files.list(server.getServerDirectory().resolve("logs"))) {
			for (Path log : (Iterable<Path>) logs::iterator) {
				String name = log.getFileName().toString();
				if (name.matches("\\d{4}-\\d{2}-\\d{2}-\\d+\\.log\\.gz")) {
					String day = name.substring(0, 10);
					if (first == null || day.compareTo(first) < 0) {
						first = day;
					}
				}
			}
		} catch (IOException ignored) {
			// no logs folder
		}
		try {
			JsonObject days = JsonParser.parseString(Files.readString(server.getServerDirectory().resolve("history.json"))).getAsJsonObject().getAsJsonObject("days");
			for (String day : days.keySet()) {
				if (first == null || day.compareTo(first) < 0) {
					first = day;
				}
			}
		} catch (Exception ignored) {
			// no history yet
		}
		return first;
	}

	// ---------------------------------------------------------------- actions

	private static String cleanName(JsonObject body) {
		String name = body.has("name") ? body.get("name").getAsString().strip() : "";
		if (name.length() > MAX_NAME || name.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("A név legfeljebb " + MAX_NAME + " karakter lehet");
		}
		return name;
	}

	/** Gives a world its own name in the panel; an empty name goes back to the game's own. Runs on the server thread. */
	Map<String, Object> rename(MinecraftServer server, JsonObject body) {
		String id = body.has("id") ? body.get("id").getAsString() : "";
		boolean known = mainId(server).equals(id) || extra(server).stream().anyMatch(e -> e.id().toString().equals(id));
		if (!known) {
			throw new IllegalArgumentException("Ismeretlen világ");
		}
		String name = cleanName(body);
		JsonObject entry = metaOf(server, id);
		if (name.isEmpty()) {
			entry.remove("name");
		} else {
			entry.addProperty("name", name);
		}
		saveMeta(server, id, entry);
		return Map.of("ok", true);
	}

	/** Starts making a new world with its own Nether and End; the list shows it as being made until it is ready. */
	Map<String, Object> create(MinecraftServer server, JsonObject body) {
		if (!available()) {
			throw new IllegalArgumentException("Ehhez a Worlds mod kell a szerverre");
		}
		String name = cleanName(body);
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Adj nevet a világnak");
		}
		String type = body.has("type") ? body.get("type").getAsString() : "normal";
		ResourceKey<WorldPreset> preset = TYPES.get(type);
		if (preset == null) {
			throw new IllegalArgumentException("Ismeretlen világtípus");
		}
		boolean netherAndEnd = !body.has("netherEnd") || body.get("netherEnd").getAsBoolean();
		String seedText = body.has("seed") ? body.get("seed").getAsString().strip() : "";
		Long seed = seedText.isEmpty() ? null : parseSeed(seedText);

		String slug = slug(name);
		Identifier id = Identifier.fromNamespaceAndPath(NAMESPACE, slug);
		for (int i = 2; MultiWorlds.exists(server, id) || pending.containsKey(id.toString()) || level(server, id) != null; i++) {
			id = Identifier.fromNamespaceAndPath(NAMESPACE, slug + "_" + i);
		}
		String key = id.toString();
		JsonObject entry = new JsonObject();
		entry.addProperty("name", name);
		entry.addProperty("type", type);
		entry.addProperty("created", System.currentTimeMillis());
		if (seed != null) {
			entry.addProperty("seed", String.valueOf(seed));
		}
		saveMeta(server, key, entry);
		pending.put(key, "");
		EndRealms.LOGGER.info("Control panel: creating world {} ({}, nether and end: {})", key, type, netherAndEnd);
		MultiWorlds.create(server, id, preset, seed, netherAndEnd, failure -> {
			if (failure == null) {
				pending.remove(key);
				EndRealms.LOGGER.info("Control panel: world {} is ready", key);
			} else {
				Throwable cause = failure instanceof java.util.concurrent.CompletionException && failure.getCause() != null ? failure.getCause() : failure;
				pending.put(key, String.valueOf(cause.getMessage()));
				EndRealms.LOGGER.warn("Control panel: could not create world {}", key, cause);
			}
		});
		return Map.of("ok", true, "id", key);
	}

	/** Like the game's own: a number is used as it is, any other text by its hash. */
	private static long parseSeed(String text) {
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException exception) {
			return text.hashCode();
		}
	}

	/** "Kalandvilág 2!" -> "kalandvilag_2": what may stand in a dimension id. */
	static String slug(String name) {
		String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
		String slug = plain.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		if (slug.isEmpty()) {
			slug = "vilag";
		}
		return slug.length() > 32 ? slug.substring(0, 32) : slug;
	}

	/** Sends an online player to a world's spawn; their inventory switches with the world's group. */
	Map<String, Object> teleport(MinecraftServer server, JsonObject body) {
		if (!available()) {
			throw new IllegalArgumentException("Ehhez a Worlds mod kell a szerverre");
		}
		String playerName = body.has("player") ? body.get("player").getAsString() : "";
		ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
		if (player == null) {
			throw new IllegalArgumentException("Ez a játékos nincs fent");
		}
		String id = body.has("id") ? body.get("id").getAsString() : "";
		Identifier target;
		if (mainId(server).equals(id)) {
			target = Level.OVERWORLD.identifier();
		} else {
			MultiWorlds.Entry entry = extra(server).stream().filter(e -> e.id().toString().equals(id)).findFirst().orElse(null);
			if (entry == null || entry.overworld() == null) {
				throw new IllegalArgumentException("Ismeretlen világ");
			}
			target = entry.overworld();
		}
		if (!MultiWorlds.teleport(server, player, target)) {
			throw new IllegalArgumentException("Nem sikerült átküldeni");
		}
		return Map.of("ok", true);
	}
}
