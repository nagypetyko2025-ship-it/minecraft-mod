package hu.endrealms.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.storage.LevelResource;

/** One player's page in the control panel: their statistics, advancements and where they are (or logged out). */
final class PlayerDetails {
	private static final int TOP = 6;
	private JsonObject hungarian;

	Map<String, Object> describe(MinecraftServer server, UUID id, String name, ServerHistory history) {
		ServerPlayer online = server.getPlayerList().getPlayer(id);
		JsonObject stats = online != null ? liveStats(online.getStats()) : statsFile(server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id + ".json"));
		JsonObject custom = group(stats, "minecraft:custom");

		Map<String, Object> d = new LinkedHashMap<>();
		d.put("name", name);
		d.put("uuid", id.toString());
		d.put("online", online != null);
		d.put("days", history.daysOf(name));

		Map<String, Object> s = new LinkedHashMap<>();
		s.put("playTicks", value(custom, "play_time"));
		s.put("deaths", value(custom, "deaths"));
		s.put("playerKills", value(custom, "player_kills"));
		s.put("mobKills", value(custom, "mob_kills"));
		s.put("damageDealt", value(custom, "damage_dealt"));
		s.put("damageTaken", value(custom, "damage_taken"));
		long cm = 0;
		for (Map.Entry<String, JsonElement> entry : custom.entrySet()) {
			if (entry.getKey().endsWith("_one_cm") && !entry.getKey().endsWith("fall_one_cm")) {
				cm += entry.getValue().getAsLong();
			}
		}
		s.put("distanceCm", cm);
		s.put("jumps", value(custom, "jump"));
		s.put("sinceDeathTicks", value(custom, "time_since_death"));
		s.put("logins", value(custom, "leave_game") + (online != null ? 1 : 0));
		s.put("trades", value(custom, "traded_with_villager"));
		s.put("fish", value(custom, "fish_caught"));
		s.put("bred", value(custom, "animals_bred"));
		s.put("enchanted", value(custom, "enchant_item"));
		s.put("mined", total(group(stats, "minecraft:mined")));
		s.put("crafted", total(group(stats, "minecraft:crafted")));
		s.put("placed", total(group(stats, "minecraft:used")));
		d.put("stats", s);
		d.put("topKilled", top(group(stats, "minecraft:killed"), "entity"));
		d.put("topKilledBy", top(group(stats, "minecraft:killed_by"), "entity"));
		d.put("topMined", top(group(stats, "minecraft:mined"), "block"));
		d.put("advancements", advancements(server, id, online));
		d.put("place", online != null ? livePlace(online) : savedPlace(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id + ".dat")));
		return d;
	}

	// ---------------------------------------------------------------- statistics, in the stats file's own shape

	private static JsonObject liveStats(StatsCounter counter) {
		JsonObject all = new JsonObject();
		for (StatType<?> type : BuiltInRegistries.STAT_TYPE) {
			collect(all, counter, type);
		}
		return all;
	}

	private static <T> void collect(JsonObject all, StatsCounter counter, StatType<T> type) {
		JsonObject group = new JsonObject();
		for (Stat<T> stat : type) {
			int v = counter.getValue(stat);
			if (v != 0) {
				group.addProperty(String.valueOf(type.getRegistry().getKey(stat.getValue())), v);
			}
		}
		if (group.size() > 0) {
			all.add(String.valueOf(BuiltInRegistries.STAT_TYPE.getKey(type)), group);
		}
	}

	private static JsonObject statsFile(Path file) {
		try {
			return JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("stats");
		} catch (Exception exception) {
			return new JsonObject();
		}
	}

	private static JsonObject group(JsonObject stats, String key) {
		return stats != null && stats.has(key) ? stats.getAsJsonObject(key) : new JsonObject();
	}

	private static long value(JsonObject group, String key) {
		JsonElement v = group.get("minecraft:" + key);
		return v == null ? 0 : v.getAsLong();
	}

	private static long total(JsonObject group) {
		long sum = 0;
		for (Map.Entry<String, JsonElement> entry : group.entrySet()) {
			sum += entry.getValue().getAsLong();
		}
		return sum;
	}

	private List<Map<String, Object>> top(JsonObject group, String kind) {
		List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(group.entrySet());
		entries.sort((a, b) -> Long.compare(b.getValue().getAsLong(), a.getValue().getAsLong()));
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map.Entry<String, JsonElement> entry : entries.subList(0, Math.min(TOP, entries.size()))) {
			Map<String, Object> e = new LinkedHashMap<>();
			e.put("id", entry.getKey());
			int colon = entry.getKey().indexOf(':');
			e.put("name", translate(kind + "." + entry.getKey().substring(0, colon) + "." + entry.getKey().substring(colon + 1)));
			e.put("count", entry.getValue().getAsLong());
			list.add(e);
		}
		return list;
	}

	// ---------------------------------------------------------------- advancements

	private Map<String, Object> advancements(MinecraftServer server, UUID id, ServerPlayer online) {
		JsonObject saved = new JsonObject();
		if (online == null) {
			try {
				saved = JsonParser.parseString(Files.readString(server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(id + ".json"))).getAsJsonObject();
			} catch (Exception ignored) {
				// never played long enough to save any
			}
		}
		int total = 0;
		List<String> earned = new ArrayList<>();
		for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
			if (holder.value().display().isEmpty() || holder.id().getPath().startsWith("recipes/")) {
				continue;
			}
			total++;
			boolean done;
			if (online != null) {
				done = online.getAdvancements().getOrStartProgress(holder).isDone();
			} else {
				JsonElement entry = saved.get(holder.id().toString());
				done = entry != null && entry.isJsonObject() && entry.getAsJsonObject().has("done") && entry.getAsJsonObject().get("done").getAsBoolean();
			}
			if (done) {
				earned.add(translate(holder.value().display().get().title()));
			}
		}
		earned.sort(String.CASE_INSENSITIVE_ORDER);
		Map<String, Object> a = new LinkedHashMap<>();
		a.put("done", earned.size());
		a.put("total", total);
		a.put("earned", earned);
		return a;
	}

	// ---------------------------------------------------------------- where they are

	private static Map<String, Object> livePlace(ServerPlayer player) {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("dimension", player.level().dimension().identifier().getPath());
		p.put("x", player.getBlockX());
		p.put("y", player.getBlockY());
		p.put("z", player.getBlockZ());
		p.put("health", Math.round(player.getHealth()));
		p.put("level", player.experienceLevel);
		return p;
	}

	private static Map<String, Object> savedPlace(Path file) {
		try {
			CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			ListTag pos = tag.getListOrEmpty("Pos");
			Map<String, Object> p = new LinkedHashMap<>();
			String dimension = tag.getStringOr("Dimension", "minecraft:overworld");
			p.put("dimension", dimension.substring(dimension.indexOf(':') + 1));
			p.put("x", (int) Math.floor(pos.getDoubleOr(0, 0)));
			p.put("y", (int) Math.floor(pos.getDoubleOr(1, 0)));
			p.put("z", (int) Math.floor(pos.getDoubleOr(2, 0)));
			p.put("health", Math.round(tag.getFloatOr("Health", 20f)));
			p.put("level", tag.getIntOr("XpLevel", 0));
			return p;
		} catch (Exception exception) {
			return null;
		}
	}

	// ---------------------------------------------------------------- names: the mod's own in Hungarian, the game's in English

	private String translate(Component component) {
		if (component.getContents() instanceof TranslatableContents contents) {
			return translate(contents.getKey());
		}
		return component.getString();
	}

	private String translate(String key) {
		if (hungarian == null) {
			hungarian = new JsonObject();
			try (InputStream in = PlayerDetails.class.getResourceAsStream("/assets/endrealms/lang/hu_hu.json")) {
				if (in != null) {
					hungarian = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
				}
			} catch (Exception ignored) {
				// English names then
			}
		}
		if (hungarian.has(key)) {
			return hungarian.get(key).getAsString();
		}
		return Language.getInstance().getOrDefault(key);
	}
}
