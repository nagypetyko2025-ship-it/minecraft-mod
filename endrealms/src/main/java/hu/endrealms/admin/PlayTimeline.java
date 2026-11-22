package hu.endrealms.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.endrealms.EndRealms;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

/**
 * sessions.json in the server folder: every time someone was online, from when to when. The control panel draws it as a
 * timeline per day, to see when friends usually play (and so when a restart bothers nobody).
 * <p>
 * Sessions still open are written every minute with the time so far, so a crash loses at most a minute. The first time,
 * the days before are filled in from the server's old logs.
 */
final class PlayTimeline {
	private static final Gson GSON = new GsonBuilder().create();
	private static final Pattern LINE = Pattern.compile(
			"^\\[(\\d\\d):(\\d\\d):(\\d\\d)\\] \\[Server thread/INFO\\][^:]*: (?:System chat: )?([A-Za-z0-9_]{1,16}) (joined|left) the game");
	private static final Pattern TIME = Pattern.compile("^\\[(\\d\\d):(\\d\\d):(\\d\\d)\\]");
	private static final Pattern LOG_NAME = Pattern.compile("(\\d{4}-\\d\\d-\\d\\d)-\\d+\\.log\\.gz");

	/** One stretch online. {@code to} moves along while the player is still on. */
	static final class Session {
		final String name;
		final long from;
		long to;
		boolean open;

		Session(String name, long from, long to) {
			this.name = name;
			this.from = from;
			this.to = to;
		}
	}

	private final List<Session> sessions = new ArrayList<>();
	private final Map<String, Session> open = new HashMap<>();
	private Path file;
	private ScheduledExecutorService clock;

	void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(this::start);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> stop());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> join(handler.player.getName().getString()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> leave(handler.player.getName().getString()));
	}

	private synchronized void start(MinecraftServer server) {
		file = server.getServerDirectory().resolve("sessions.json");
		sessions.clear();
		open.clear();
		try {
			if (Files.exists(file)) {
				JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
				for (JsonElement element : root.getAsJsonArray("sessions")) {
					JsonObject s = element.getAsJsonObject();
					sessions.add(new Session(s.get("name").getAsString(), s.get("from").getAsLong(), s.get("to").getAsLong()));
				}
			} else {
				backfill(server.getServerDirectory().resolve("logs"));
				save();
			}
		} catch (Exception e) {
			EndRealms.LOGGER.warn("Could not read sessions.json, starting a new one", e);
		}
		clock = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "EndRealms-Timeline");
			thread.setDaemon(true);
			return thread;
		});
		// by the clock, not by ticks: an empty server pauses, and a full one must still be written down
		clock.scheduleWithFixedDelay(this::tick, 60, 60, TimeUnit.SECONDS);
	}

	private synchronized void stop() {
		if (clock != null) {
			clock.shutdownNow();
			clock = null;
		}
		long now = System.currentTimeMillis();
		open.values().forEach(s -> {
			s.to = now;
			s.open = false;
		});
		open.clear();
		if (file != null) {
			save();
		}
	}

	private synchronized void join(String name) {
		if (file == null || open.containsKey(name)) {
			return;
		}
		long now = System.currentTimeMillis();
		Session session = new Session(name, now, now);
		session.open = true;
		sessions.add(session);
		open.put(name, session);
		save();
	}

	private synchronized void leave(String name) {
		Session session = open.remove(name);
		if (session != null) {
			session.to = System.currentTimeMillis();
			session.open = false;
			save();
		}
	}

	private synchronized void tick() {
		if (open.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		open.values().forEach(s -> s.to = now);
		save();
	}

	/** Sessions that overlap the last {@code days} days (today included), for the panel. */
	synchronized Map<String, Object> recent(int days) {
		long since = LocalDate.now().minusDays(days - 1L).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		long now = System.currentTimeMillis();
		List<Map<String, Object>> list = new ArrayList<>();
		for (Session s : sessions) {
			long to = s.open ? now : s.to;
			if (to >= since) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("name", s.name);
				m.put("from", s.from);
				m.put("to", to);
				m.put("online", s.open);
				list.add(m);
			}
		}
		Map<String, Object> answer = new LinkedHashMap<>();
		answer.put("since", since);
		answer.put("now", now);
		answer.put("sessions", list);
		return answer;
	}

	/**
	 * Rebuilds the past from the server's old logs (logs/2026-11-20-3.log.gz and so on): joins and leaves with their
	 * times; a log that ends with players still on closes them at its last line, where the server stopped.
	 */
	private void backfill(Path logs) {
		List<Path> files = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(logs, "*.log.gz")) {
			stream.forEach(files::add);
		} catch (IOException e) {
			return;
		}
		files.sort((a, b) -> {
			Matcher x = LOG_NAME.matcher(a.getFileName().toString());
			Matcher y = LOG_NAME.matcher(b.getFileName().toString());
			if (!x.matches() || !y.matches()) {
				return a.compareTo(b);
			}
			int c = x.group(1).compareTo(y.group(1));
			return c != 0 ? c : Integer.compare(number(a), number(b));
		});
		ZoneId zone = ZoneId.systemDefault();
		for (Path log : files) {
			Matcher name = LOG_NAME.matcher(log.getFileName().toString());
			if (!name.matches()) {
				continue;
			}
			LocalDate day = LocalDate.parse(name.group(1));
			Map<String, Long> on = new HashMap<>();
			long last = 0;
			long previous = -1;
			try (InputStream in = new GZIPInputStream(Files.newInputStream(log));
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				for (String line; (line = reader.readLine()) != null; ) {
					Matcher time = TIME.matcher(line);
					if (!time.find()) {
						continue;
					}
					long seconds = Integer.parseInt(time.group(1)) * 3600L + Integer.parseInt(time.group(2)) * 60L + Integer.parseInt(time.group(3));
					if (previous >= 0 && seconds < previous - 3600) {
						day = day.plusDays(1);                     // the server ran past midnight
					}
					previous = seconds;
					last = day.atTime(LocalTime.ofSecondOfDay(seconds)).atZone(zone).toInstant().toEpochMilli();
					Matcher m = LINE.matcher(line);
					if (m.find()) {
						String player = m.group(4);
						if (m.group(5).equals("joined")) {
							on.putIfAbsent(player, last);
						} else if (on.containsKey(player)) {
							sessions.add(new Session(player, on.remove(player), last));
						}
					}
				}
			} catch (IOException | RuntimeException e) {
				EndRealms.LOGGER.warn("Could not read {} for the play timeline", log.getFileName(), e);
			}
			for (Map.Entry<String, Long> still : on.entrySet()) {
				sessions.add(new Session(still.getKey(), still.getValue(), last));
			}
		}
		sessions.sort((a, b) -> Long.compare(a.from, b.from));
		EndRealms.LOGGER.info("Play timeline: filled in {} sessions from {} old logs", sessions.size(), files.size());
	}

	private static int number(Path log) {
		String n = log.getFileName().toString();
		n = n.substring(11, n.indexOf(".log"));
		try {
			return Integer.parseInt(n);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void save() {
		JsonObject root = new JsonObject();
		JsonArray list = new JsonArray();
		for (Session s : sessions) {
			JsonObject o = new JsonObject();
			o.addProperty("name", s.name);
			o.addProperty("from", s.from);
			o.addProperty("to", s.to);
			list.add(o);
		}
		root.add("sessions", list);
		try {
			Path temp = file.resolveSibling("sessions.json.tmp");
			Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
			Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			EndRealms.LOGGER.warn("Could not write sessions.json", e);
		}
	}
}
