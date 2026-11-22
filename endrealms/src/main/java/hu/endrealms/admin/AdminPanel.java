package hu.endrealms.admin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hu.endrealms.EndRealms;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The server's control panel: a small web page served only on this computer (127.0.0.1). Reaching the API takes a
 * secret token, which is stored in the PANEL.url shortcut next to the server; the playit tunnel never exposes this port.
 */
public class AdminPanel implements DedicatedServerModInitializer {
	private static final int PORT = Integer.getInteger("endrealms.panelPort", 25580);     // another port only for test servers
	private static final Gson GSON = new Gson();
	private static final Pattern PLAYER = Pattern.compile("[A-Za-z0-9_]{1,16}");
	private static final Pattern RULE = Pattern.compile("[a-z0-9_:.]{1,80}");
	private static final Set<String> SETTINGS = Set.of("motd", "max-players", "view-distance", "simulation-distance", "difficulty", "gamemode", "spawn-protection", "allow-flight",
			"white-list", "enforce-whitelist", "player-idle-timeout", "hide-online-players", "pause-when-empty-seconds", "force-gamemode");
	private static final int LOG_LINES = 200;

	private MinecraftServer server;
	private HttpServer http;
	private ExecutorService executor;
	private String token;
	private long startedAt;
	private JsonObject ruleNames = new JsonObject();
	private final ServerHistory history = new ServerHistory();
	private final PlayTimeline timeline = new PlayTimeline();
	private final PlayerDetails details = new PlayerDetails();
	private final Worlds worlds = new Worlds();

	@Override
	public void onInitializeServer() {
		ServerLifecycleEvents.SERVER_STARTED.register(this::start);
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> stop());
		history.register();
		timeline.register();
		AutoRestart.register();
		if (Worlds.available()) {
			MultiWorlds.register();
		}
	}

	private void start(MinecraftServer started) {
		server = started;
		startedAt = System.currentTimeMillis();
		try (InputStream in = AdminPanel.class.getResourceAsStream("/admin/gamerules_hu.json")) {
			if (in != null) {
				ruleNames = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			}
			token = loadToken();
			http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT), 0);
			http.createContext("/", this::handle);
			executor = Executors.newFixedThreadPool(3, runnable -> {
				Thread thread = new Thread(runnable, "EndRealms-Panel");
				thread.setDaemon(true);
				return thread;
			});
			http.setExecutor(executor);
			http.start();
			Files.writeString(serverFile("PANEL.url"), "[InternetShortcut]\r\nURL=http://127.0.0.1:" + PORT + "/?t=" + token + "\r\n");
			EndRealms.LOGGER.info("Control panel ready: open PANEL.url in the server folder");
		} catch (IOException exception) {
			EndRealms.LOGGER.error("Could not start the control panel", exception);
		}
	}

	private void stop() {
		if (http != null) {
			http.stop(0);
			executor.shutdownNow();
			http = null;
		}
	}

	private Path serverFile(String name) {
		return server.getServerDirectory().resolve(name);
	}

	private String loadToken() throws IOException {
		Path file = serverFile("config").resolve("endrealms-panel.token");
		if (Files.exists(file)) {
			return Files.readString(file).trim();
		}
		byte[] bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		String fresh = HexFormat.of().formatHex(bytes);
		Files.createDirectories(file.getParent());
		Files.writeString(file, fresh);
		return fresh;
	}

	// ---------------------------------------------------------------- HTTP

	private void send(HttpExchange exchange, int code, String type, byte[] body) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", type);
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
		exchange.sendResponseHeaders(code, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private void json(HttpExchange exchange, int code, Object value) throws IOException {
		send(exchange, code, "application/json; charset=utf-8", GSON.toJson(value).getBytes(StandardCharsets.UTF_8));
	}

	private void handle(HttpExchange exchange) throws IOException {
		try {
			String host = exchange.getRequestHeaders().getFirst("Host");
			if (host == null || !(host.equals("127.0.0.1:" + PORT) || host.equals("localhost:" + PORT))) {
				json(exchange, 403, Map.of("error", "forbidden"));
				return;
			}
			String path = exchange.getRequestURI().getPath();
			if (path.equals("/")) {
				try (InputStream in = AdminPanel.class.getResourceAsStream("/admin/panel.html")) {
					send(exchange, 200, "text/html; charset=utf-8", in.readAllBytes());
				}
				return;
			}
			if (!token.equals(exchange.getRequestHeaders().getFirst("X-Token"))) {
				json(exchange, 401, Map.of("error", "token"));
				return;
			}
			boolean post = exchange.getRequestMethod().equals("POST");
			JsonObject body = post ? JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
			Object answer = switch (path) {
				case "/api/status" -> onServer(this::status);
				case "/api/players" -> onServer(this::players);
				case "/api/whitelist" -> onServer(this::whitelist);
				case "/api/timeline" -> {
					String query = exchange.getRequestURI().getQuery();
					int days = 7;
					if (query != null && query.matches("days=\\d{1,2}")) {
						days = Math.max(1, Math.min(31, Integer.parseInt(query.substring(5))));
					}
					yield timeline.recent(days);
				}
				case "/api/worlds" -> {
					worlds.measure(onServer(() -> worlds.folders(server)));
					yield onServer(() -> worlds.list(server));
				}
				case "/api/world" -> {
					String query = exchange.getRequestURI().getRawQuery() == null ? "" : exchange.getRequestURI().getRawQuery().replaceFirst("^id=", "");
					String id = java.net.URLDecoder.decode(query, StandardCharsets.UTF_8);
					worlds.measure(onServer(() -> worlds.folders(server)));
					yield onServer(() -> worlds.describe(server, id));     // null -> 404
				}
				case "/api/world-rename" -> onServer(() -> worlds.rename(server, body));
				case "/api/world-create" -> onServer(() -> worlds.create(server, body));
				case "/api/world-tp" -> onServer(() -> worlds.teleport(server, body));
				case "/api/player" -> {
					String uuid = exchange.getRequestURI().getQuery() == null ? "" : exchange.getRequestURI().getQuery().replaceFirst("^uuid=", "");
					java.util.UUID id = java.util.UUID.fromString(uuid);     // IllegalArgumentException -> 400
					yield onServer(() -> {
						String name = knownNames().get(id.toString());
						return name == null ? null : details.describe(server, id, name, history);     // null -> 404
					});
				}
				case "/api/gamerules" -> onServer(this::gamerules);
				case "/api/settings" -> post ? saveSettings(body) : settings();
				case "/api/log" -> log();
				case "/api/gamerule" -> setRule(body);
				case "/api/action" -> action(body);
				case "/api/command" -> console(body);
				default -> null;
			};
			if (answer == null) {
				json(exchange, 404, Map.of("error", "not found"));
			} else {
				json(exchange, 200, answer);
			}
		} catch (IllegalArgumentException exception) {
			json(exchange, 400, Map.of("error", String.valueOf(exception.getMessage())));
		} catch (Exception exception) {
			EndRealms.LOGGER.warn("Control panel request failed", exception);
			json(exchange, 500, Map.of("error", "server"));
		}
	}

	private <T> T onServer(java.util.function.Supplier<T> task) throws Exception {
		try {
			return server.submit(task).get(5, TimeUnit.SECONDS);
		} catch (java.util.concurrent.ExecutionException exception) {
			if (exception.getCause() instanceof IllegalArgumentException cause) {
				throw cause;     // a bad request, answered with its message
			}
			throw exception;
		}
	}

	private void run(String command) {
		server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
	}

	private static String player(JsonObject body) {
		String name = body.has("player") ? body.get("player").getAsString() : "";
		if (!PLAYER.matcher(name).matches()) {
			throw new IllegalArgumentException("Érvénytelen játékosnév");
		}
		return name;
	}

	// ---------------------------------------------------------------- data

	private Map<String, Object> status() {
		ServerLevel overworld = server.overworld();
		double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
		List<Map<String, Object>> online = new ArrayList<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Map<String, Object> p = new LinkedHashMap<>();
			p.put("name", player.getGameProfile().name());
			p.put("ping", player.connection.latency());
			p.put("mode", player.gameMode().getName());
			p.put("dimension", player.level().dimension().identifier().getPath());
			p.putAll(worlds.whereIs(server, player.level().dimension().identifier()));     // world, worldId, kind
			p.put("x", player.getBlockX());
			p.put("y", player.getBlockY());
			p.put("z", player.getBlockZ());
			p.put("health", Math.round(player.getHealth()));
			online.add(p);
		}
		Runtime runtime = Runtime.getRuntime();
		Map<String, Object> s = new LinkedHashMap<>();
		s.put("motd", server.getMotd());
		s.put("max", server.getPlayerList().getMaxPlayers());
		s.put("online", online);
		s.put("mspt", Math.round(mspt * 10.0) / 10.0);
		s.put("tps", Math.round(Math.min(20.0, 1000.0 / Math.max(mspt, 0.1)) * 10.0) / 10.0);
		s.put("memUsed", (runtime.totalMemory() - runtime.freeMemory()) / 1048576);
		s.put("memMax", runtime.maxMemory() / 1048576);
		s.put("uptime", (System.currentTimeMillis() - startedAt) / 1000);
		s.put("time", overworld.getOverworldClockTime() % 24000L);
		s.put("day", overworld.getOverworldClockTime() / 24000L);
		s.put("weather", overworld.isThundering() ? "thunder" : overworld.isRaining() ? "rain" : "clear");
		s.put("difficulty", overworld.getDifficulty().name().toLowerCase(Locale.ROOT));
		return s;
	}

	private JsonArray readArray(String file) {
		try {
			return JsonParser.parseString(Files.readString(serverFile(file))).getAsJsonArray();
		} catch (Exception exception) {
			return new JsonArray();
		}
	}

	private Set<String> names(String file) {
		Set<String> set = new HashSet<>();
		for (JsonElement element : readArray(file)) {
			set.add(element.getAsJsonObject().get("name").getAsString().toLowerCase(Locale.ROOT));
		}
		return set;
	}

	/** Turned away by the whitelist: "Disconnecting Name (/1.2.3.4:5678): You are not white-listed on this server!" */
	private static final Pattern REFUSED = Pattern.compile("Disconnecting ([A-Za-z0-9_]{1,16}) \\(.*not white-listed");

	/**
	 * The whitelist tab: who is on it (and whether they are online or when they last played), who tried to join today and
	 * was turned away, and who has played before but is not on the list, so either can be added with one click.
	 */
	private Map<String, Object> whitelist() {
		Map<String, Map<String, Object>> known = new LinkedHashMap<>();
		for (Map<String, Object> p : players()) {
			known.put(((String) p.get("name")).toLowerCase(Locale.ROOT), p);
		}
		List<Map<String, Object>> listed = new ArrayList<>();
		Set<String> onList = new HashSet<>();
		for (String name : server.getPlayerList().getWhiteListNames()) {
			onList.add(name.toLowerCase(Locale.ROOT));
			Map<String, Object> p = known.get(name.toLowerCase(Locale.ROOT));
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("name", name);
			entry.put("online", p != null && (Boolean) p.get("online"));
			entry.put("lastPlayed", p == null ? 0L : p.get("lastPlayed"));
			listed.add(entry);
		}
		listed.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
		List<String> refused = new ArrayList<>();
		Path log = serverFile("logs").resolve("latest.log");
		try {
			for (String line : Files.exists(log) ? Files.readAllLines(log, StandardCharsets.UTF_8) : List.<String>of()) {
				java.util.regex.Matcher m = REFUSED.matcher(line);
				if (m.find()) {
					String name = m.group(1);
					refused.remove(name);
					refused.add(0, name);       // newest first
				}
			}
		} catch (IOException ignored) {
		}
		refused.removeIf(name -> onList.contains(name.toLowerCase(Locale.ROOT)));
		List<String> others = new ArrayList<>();
		for (Map<String, Object> p : known.values()) {
			String name = (String) p.get("name");
			if (!onList.contains(name.toLowerCase(Locale.ROOT)) && !refused.contains(name)) {
				others.add(name);
			}
		}
		Map<String, Object> answer = new LinkedHashMap<>();
		answer.put("enabled", server.getPlayerList().isUsingWhitelist());
		answer.put("listed", listed);
		answer.put("refused", refused);
		answer.put("others", others);
		return answer;
	}

	private List<Map<String, Object>> players() {
		Set<String> online = new HashSet<>();
		server.getPlayerList().getPlayers().forEach(p -> online.add(p.getGameProfile().name().toLowerCase(Locale.ROOT)));
		Set<String> ops = names("ops.json");
		Set<String> banned = names("banned-players.json");
		Set<String> whitelisted = names("whitelist.json");
		Path data = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
		Path stats = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map.Entry<String, String> entry : knownNames().entrySet()) {
			String uuid = entry.getKey();
			String name = entry.getValue();
			Path file = data.resolve(uuid + ".dat");
			String lower = name.toLowerCase(Locale.ROOT);
			if (!Files.exists(file) && !online.contains(lower)) {
				continue;
			}
			Map<String, Object> p = new LinkedHashMap<>();
			p.put("name", name);
			p.put("uuid", uuid);
			p.put("online", online.contains(lower));
			p.put("op", ops.contains(lower));
			p.put("banned", banned.contains(lower));
			p.put("whitelisted", whitelisted.contains(lower));
			try {
				p.put("lastPlayed", Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : System.currentTimeMillis());
			} catch (IOException exception) {
				p.put("lastPlayed", 0L);
			}
			p.put("playMinutes", playMinutes(stats.resolve(uuid + ".json")));
			list.add(p);
		}
		list.sort((a, b) -> Long.compare((Long) b.get("lastPlayed"), (Long) a.get("lastPlayed")));
		return list;
	}

	/** Everyone who ever joined: UUID -> name, from the game's name cache, the server history and whoever is online now. */
	/**
	 * Puts a player on the whitelist. This server runs in offline mode, where a player's id comes from their name, but the
	 * whitelist command looks names up at Mojang and would store the id of a paid account with that name, which never
	 * matches. So in offline mode the entry is made here, with the offline id of the name exactly as the player uses it
	 * (as the server last saw it, or as typed).
	 */
	private void whitelistAdd(String name) {
		if (server.usesAuthentication()) {
			run("whitelist add " + name);
			return;
		}
		String exact = name;
		for (Map<String, Object> played : players()) {         // only players who really played: the name cache also
			String known = (String) played.get("name");          // keeps names the whitelist command looked up at Mojang
			if (known.equalsIgnoreCase(name)) {
				exact = known;
			}
		}
		whitelistRemove(exact, false);          // drop a stale entry for the same name first
		server.getPlayerList().getWhiteList().add(new net.minecraft.server.players.UserWhiteListEntry(net.minecraft.server.players.NameAndId.createOffline(exact)));
		saveWhitelist();
		EndRealms.LOGGER.info("Control panel: added {} to the whitelist", exact);
	}

	private void whitelistRemove(String name) {
		whitelistRemove(name, true);
		ServerPlayer online = server.getPlayerList().getPlayerByName(name);
		if (online != null && server.getPlayerList().isUsingWhitelist() && !server.getPlayerList().isWhiteListed(new net.minecraft.server.players.NameAndId(online.getGameProfile()))) {
			online.connection.disconnect(net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.not_whitelisted"));
		}
	}

	/** Removes every entry with this name, whatever its id, so entries the command made with a wrong id go too. */
	private void whitelistRemove(String name, boolean log) {
		var list = server.getPlayerList().getWhiteList();
		List<net.minecraft.server.players.NameAndId> gone = new ArrayList<>();
		for (var entry : list.getEntries()) {
			if (entry.getUser() != null && entry.getUser().name().equalsIgnoreCase(name)) {
				gone.add(entry.getUser());
			}
		}
		gone.forEach(list::remove);
		if (!gone.isEmpty()) {
			saveWhitelist();
			if (log) {
				EndRealms.LOGGER.info("Control panel: removed {} from the whitelist", name);
			}
		}
	}

	private void saveWhitelist() {
		try {
			server.getPlayerList().getWhiteList().save();
		} catch (IOException e) {
			EndRealms.LOGGER.error("Could not save the whitelist", e);
		}
	}

	private Map<String, String> knownNames() {
		Map<String, String> names = new LinkedHashMap<>(history.names());
		for (JsonElement element : readArray("usercache.json")) {
			JsonObject entry = element.getAsJsonObject();
			names.putIfAbsent(entry.get("uuid").getAsString(), entry.get("name").getAsString());
		}
		server.getPlayerList().getPlayers().forEach(p -> names.put(p.getUUID().toString(), p.getGameProfile().name()));
		return names;
	}

	private static long playMinutes(Path statsFile) {
		try {
			JsonObject stats = JsonParser.parseString(Files.readString(statsFile)).getAsJsonObject().getAsJsonObject("stats").getAsJsonObject("minecraft:custom");
			return stats.get("minecraft:play_time").getAsLong() / 1200L;
		} catch (Exception exception) {
			return 0L;
		}
	}

	private List<Map<String, Object>> gamerules() {
		GameRules rules = server.getGameRules();
		List<Map<String, Object>> list = new ArrayList<>();
		rules.availableRules().forEach(rule -> list.add(describe(rules, rule)));
		list.sort((a, b) -> ((String) a.get("label")).compareToIgnoreCase((String) b.get("label")));
		return list;
	}

	private <T> Map<String, Object> describe(GameRules rules, GameRule<T> rule) {
		Map<String, Object> r = new LinkedHashMap<>();
		String key = rule.getIdentifierWithFallback().getPath();
		r.put("id", rule.getIdentifierWithFallback().toString());
		r.put("label", ruleNames.has(key) ? ruleNames.get(key).getAsString() : rule.id());
		r.put("bool", rule.valueClass() == Boolean.class);
		r.put("value", rules.getAsString(rule));
		r.put("default", rule.serialize(rule.defaultValue()));
		return r;
	}

	private Map<String, Object> setRule(JsonObject body) {
		String id = body.get("id").getAsString();
		String value = body.get("value").getAsString();
		if (!RULE.matcher(id).matches() || !(value.equals("true") || value.equals("false") || value.matches("-?\\d{1,9}"))) {
			throw new IllegalArgumentException("Érvénytelen szabályérték");
		}
		run("gamerule " + id + " " + value);
		return Map.of("ok", true);
	}

	private Map<String, Object> action(JsonObject body) throws IOException {
		String action = body.get("action").getAsString();
		switch (action) {
			case "kick" -> run("kick " + player(body));
			case "ban" -> run("ban " + player(body));
			case "pardon" -> run("pardon " + player(body));
			case "op" -> run("op " + player(body));
			case "deop" -> run("deop " + player(body));
			case "whitelist-add" -> {
				String name = player(body);
				server.execute(() -> whitelistAdd(name));
			}
			case "whitelist-remove" -> {
				String name = player(body);
				server.execute(() -> whitelistRemove(name));      // also sends them off if they are online
			}
			case "whitelist-on" -> run("whitelist on");
			case "whitelist-off" -> run("whitelist off");
			case "survival", "creative", "adventure", "spectator" -> run("gamemode " + action + " " + player(body));
			case "day" -> run("time set day");
			case "night" -> run("time set night");
			case "clear", "rain", "thunder" -> run("weather " + action);
			case "peaceful", "easy", "normal", "hard" -> run("difficulty " + action);
			case "save" -> run("save-all");
			case "say" -> {
				String text = body.get("text").getAsString().replaceAll("[\\r\\n]", " ").trim();
				if (text.isEmpty() || text.length() > 200) {
					throw new IllegalArgumentException("Üres vagy túl hosszú üzenet");
				}
				run("say " + text);
			}
			case "restart" -> server.execute(() -> AutoRestart.restart(server, "asked for in the control panel"));
			case "shutdown" -> {
				Files.writeString(serverFile("stop.flag"), "panel");
				run("stop");
			}
			default -> throw new IllegalArgumentException("Ismeretlen művelet");
		}
		return Map.of("ok", true);
	}

	private Map<String, Object> console(JsonObject body) {
		String command = body.get("command").getAsString().replaceAll("[\\r\\n]", " ").trim();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}
		if (command.isEmpty()) {
			throw new IllegalArgumentException("Üres parancs");
		}
		run(command);
		return Map.of("ok", true);
	}

	private Map<String, Object> log() throws IOException {
		Path file = serverFile("logs").resolve("latest.log");
		List<String> lines = Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of();
		return Map.of("lines", lines.subList(Math.max(0, lines.size() - LOG_LINES), lines.size()));
	}

	private Map<String, String> settings() throws IOException {
		Map<String, String> values = new LinkedHashMap<>();
		for (String line : Files.readAllLines(serverFile("server.properties"), StandardCharsets.UTF_8)) {
			int split = line.indexOf('=');
			if (split > 0 && SETTINGS.contains(line.substring(0, split))) {
				values.put(line.substring(0, split), line.substring(split + 1));
			}
		}
		return values;
	}

	private Map<String, Object> saveSettings(JsonObject body) throws IOException {
		List<String> lines = new ArrayList<>(Files.readAllLines(serverFile("server.properties"), StandardCharsets.UTF_8));
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			int split = line.indexOf('=');
			if (split > 0 && SETTINGS.contains(line.substring(0, split)) && body.has(line.substring(0, split))) {
				String value = body.get(line.substring(0, split)).getAsString().replaceAll("[\\r\\n]", " ");
				lines.set(i, line.substring(0, split + 1) + value);
			}
		}
		Files.write(serverFile("server.properties"), lines, StandardCharsets.UTF_8);
		return Map.of("ok", true);
	}
}
