package hu.endrealms.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.tree.CommandNode;
import hu.endrealms.EndRealms;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * Ranks: named, coloured groups of players, kept in config/endrealms-ranks.json and edited in the control panel.
 * <p>
 * A player may have any number of ranks. Their tags show before the player's name in the Tab list, in chat and on the
 * name tag above their head (through a scoreboard team of their own), with no client mod needed; higher ranks (lower
 * order number) sort first in the Tab list. Ranks may also open commands to their members that would otherwise need
 * operator rights: every top-level command's requirement is widened once to "the old rule, or one of the player's
 * ranks allows this command".
 */
public final class Ranks {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String TEAM_PREFIX = "rank_";
	private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,24}");
	private static final Pattern COLOUR = Pattern.compile("#[0-9a-fA-F]{6}");
	private static final Pattern PLAYER = Pattern.compile("[A-Za-z0-9_]{1,16}");

	/** One rank: shown as "[name] " in its colour; order decides the Tab list (smaller first). */
	public record Rank(String id, String name, String color, int order, boolean bold, List<String> commands, List<String> members) {
	}

	private static final Map<String, Rank> RANKS = new LinkedHashMap<>();
	/** Lower-case player name -> their rank ids, highest rank first. */
	private static final Map<String, List<String>> MEMBER_OF = new HashMap<>();
	private static Path file;
	private static boolean wrapped;

	private Ranks() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			file = server.getServerDirectory().resolve("config").resolve("endrealms-ranks.json");
			load();
			wrapCommands(server);
			apply(server);
		});
		// a data pack reload builds the command tree anew
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
			wrapped = false;
			wrapCommands(server);
			server.getPlayerList().getPlayers().forEach(p -> server.getCommands().sendCommands(p));
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> server.getCommands().sendCommands(handler.player)));
	}

	// ---------------------------------------------------------------- storage

	private static synchronized void load() {
		RANKS.clear();
		try {
			if (Files.exists(file)) {
				JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
				for (JsonElement e : root.getAsJsonArray("ranks")) {
					Rank rank = parse(e.getAsJsonObject());
					RANKS.put(rank.id(), rank);
				}
			}
		} catch (Exception e) {
			EndRealms.LOGGER.warn("Could not read endrealms-ranks.json", e);
		}
		reindex();
	}

	private static void save() {
		JsonArray list = new JsonArray();
		RANKS.values().forEach(rank -> list.add(json(rank)));
		JsonObject root = new JsonObject();
		root.add("ranks", list);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			EndRealms.LOGGER.warn("Could not write endrealms-ranks.json", e);
		}
	}

	private static void reindex() {
		MEMBER_OF.clear();
		for (Rank rank : sorted()) {
			for (String member : rank.members()) {
				List<String> ids = MEMBER_OF.computeIfAbsent(member.toLowerCase(Locale.ROOT), k -> new ArrayList<>());
				if (!ids.contains(rank.id())) {
					ids.add(rank.id());
				}
			}
		}
	}

	private static List<Rank> sorted() {
		List<Rank> list = new ArrayList<>(RANKS.values());
		list.sort(Comparator.comparingInt(Rank::order).thenComparing(Rank::id));
		return list;
	}

	private static List<String> strings(JsonObject o, String key) {
		List<String> list = new ArrayList<>();
		if (o.has(key)) {
			o.getAsJsonArray(key).forEach(e -> list.add(e.getAsString()));
		}
		return list;
	}

	/** Reads and checks a rank: a bad colour, id or player name is an error for the panel to show. */
	static Rank parse(JsonObject o) {
		String id = o.get("id").getAsString().trim().toLowerCase(Locale.ROOT);
		String name = o.get("name").getAsString().trim();
		String color = o.has("color") ? o.get("color").getAsString().trim() : "#FFFFFF";
		if (!ID.matcher(id).matches()) {
			throw new IllegalArgumentException("Az azonosító csak kisbetű, szám és _ lehet (max. 24)");
		}
		if (name.isEmpty() || name.length() > 24) {
			throw new IllegalArgumentException("A rang neve 1–24 karakter legyen");
		}
		if (!COLOUR.matcher(color).matches()) {
			throw new IllegalArgumentException("Érvénytelen szín: " + color);
		}
		Set<String> commands = new TreeSet<>();
		for (String c : strings(o, "commands")) {
			String root = c.trim().replaceFirst("^/", "").toLowerCase(Locale.ROOT);
			if (!root.isEmpty()) {
				commands.add(root);
			}
		}
		Set<String> members = new LinkedHashSet<>();
		for (String m : strings(o, "members")) {
			String player = m.trim();
			if (!PLAYER.matcher(player).matches()) {
				throw new IllegalArgumentException("Érvénytelen játékosnév: " + player);
			}
			members.add(player);
		}
		int order = o.has("order") ? o.get("order").getAsInt() : 100;
		boolean bold = o.has("bold") && o.get("bold").getAsBoolean();
		return new Rank(id, name, color.toUpperCase(Locale.ROOT), Math.max(0, Math.min(9999, order)), bold, new ArrayList<>(commands), new ArrayList<>(members));
	}

	static JsonObject json(Rank rank) {
		JsonObject o = new JsonObject();
		o.addProperty("id", rank.id());
		o.addProperty("name", rank.name());
		o.addProperty("color", rank.color());
		o.addProperty("order", rank.order());
		o.addProperty("bold", rank.bold());
		JsonArray commands = new JsonArray();
		rank.commands().forEach(commands::add);
		o.add("commands", commands);
		JsonArray members = new JsonArray();
		rank.members().forEach(members::add);
		o.add("members", members);
		return o;
	}

	// ---------------------------------------------------------------- for the control panel (server thread)

	static synchronized Map<String, Object> describe(MinecraftServer server) {
		List<JsonObject> ranks = new ArrayList<>();
		sorted().forEach(rank -> ranks.add(json(rank)));
		Set<String> commands = new TreeSet<>();
		for (CommandNode<CommandSourceStack> node : server.getCommands().getDispatcher().getRoot().getChildren()) {
			commands.add(node.getName());
		}
		Map<String, Object> answer = new LinkedHashMap<>();
		answer.put("ranks", ranks);
		answer.put("commands", commands);
		return answer;
	}

	/** Adds or replaces a rank. A player may be in any number of ranks. */
	static synchronized void put(MinecraftServer server, JsonObject body) {
		Rank rank = parse(body);
		RANKS.put(rank.id(), rank);
		reindex();
		save();
		apply(server);
		EndRealms.LOGGER.info("Control panel: saved rank {} ({} members)", rank.id(), rank.members().size());
	}

	static synchronized void delete(MinecraftServer server, String id) {
		if (RANKS.remove(id) != null) {
			reindex();
			save();
			apply(server);
			EndRealms.LOGGER.info("Control panel: deleted rank {}", id);
		}
	}

	/** Every rank of a player, highest (smallest order) first; empty if none. */
	public static synchronized List<Rank> of(String player) {
		List<Rank> list = new ArrayList<>();
		for (String id : MEMBER_OF.getOrDefault(player.toLowerCase(Locale.ROOT), List.of())) {
			list.add(RANKS.get(id));
		}
		return list;
	}

	// ---------------------------------------------------------------- applying: teams and command access

	/**
	 * Brings the scoreboard teams in line with the ranks and re-sends everyone their command tree. As a player can be
	 * in several ranks but in one team only, every ranked player gets a team of their own, named after their highest
	 * rank's order (so the Tab list sorts by it), its prefix all their rank tags in order: "[VIP] [Repülj] ".
	 */
	static synchronized void apply(MinecraftServer server) {
		Scoreboard scoreboard = server.getScoreboard();
		Map<String, String> wanted = new HashMap<>();             // team name -> player
		for (Map.Entry<String, List<String>> entry : MEMBER_OF.entrySet()) {
			List<Rank> ranks = of(entry.getKey());
			if (ranks.isEmpty()) {
				continue;
			}
			String player = playerName(ranks.get(0), entry.getKey());
			String teamName = String.format(Locale.ROOT, "%s%04d_%s", TEAM_PREFIX, ranks.get(0).order(), entry.getKey());
			wanted.put(teamName, player);
			PlayerTeam team = scoreboard.getPlayerTeam(teamName);
			if (team == null) {
				team = scoreboard.addPlayerTeam(teamName);
			}
			net.minecraft.network.chat.MutableComponent prefix = Component.empty();
			for (Rank rank : ranks) {
				prefix.append(Component.literal("[" + rank.name() + "]").withStyle(style -> style
						.withColor(TextColor.parseColor(rank.color()).getOrThrow()).withBold(rank.bold())));
				prefix.append(Component.literal(" "));
			}
			team.setDisplayName(Component.literal(player));
			team.setPlayerPrefix(prefix);
			team.setSeeFriendlyInvisibles(false);
			team.setAllowFriendlyFire(true);
			team.setCollisionRule(Team.CollisionRule.ALWAYS);
			if (scoreboard.getPlayersTeam(player) != team) {
				scoreboard.addPlayerToTeam(player, team);
			}
		}
		for (PlayerTeam team : new ArrayList<>(scoreboard.getPlayerTeams())) {          // teams nobody needs any more
			if (team.getName().startsWith(TEAM_PREFIX) && !wanted.containsKey(team.getName())) {
				scoreboard.removePlayerTeam(team);
			}
		}
		server.getPlayerList().getPlayers().forEach(p -> server.getCommands().sendCommands(p));
	}

	/** The player's name as written in a rank's member list (the team entry must match the real name's case). */
	private static String playerName(Rank rank, String lower) {
		for (Rank r : RANKS.values()) {
			for (String m : r.members()) {
				if (m.toLowerCase(Locale.ROOT).equals(lower)) {
					return m;
				}
			}
		}
		return lower;
	}

	/** Whether any of the ranks of whoever runs a command lets them use this top-level command. */
	private static boolean allows(CommandSourceStack source, String command) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			return false;
		}
		for (Rank rank : of(player.getGameProfile().name())) {
			if (rank.commands().contains(command)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Widens every top-level command's requirement to "as before, or the player's rank allows it". The requirement is
	 * a final field of Brigadier's command node, so it is swapped by reflection, once per command tree.
	 */
	@SuppressWarnings("unchecked")
	private static void wrapCommands(MinecraftServer server) {
		if (wrapped) {
			return;
		}
		try {
			Field requirement = CommandNode.class.getDeclaredField("requirement");
			requirement.setAccessible(true);
			for (CommandNode<CommandSourceStack> node : server.getCommands().getDispatcher().getRoot().getChildren()) {
				Predicate<CommandSourceStack> original = (Predicate<CommandSourceStack>) requirement.get(node);
				String name = node.getName();
				Predicate<CommandSourceStack> widened = source -> original.test(source) || allows(source, name);
				requirement.set(node, widened);
			}
			wrapped = true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			EndRealms.LOGGER.warn("Could not open commands to ranks; ranks will only show their tag", e);
		}
	}
}
