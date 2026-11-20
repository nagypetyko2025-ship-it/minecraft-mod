package hu.endrealms.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import hu.endrealms.EndRealms;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Every named Phase Mailbox on the server, so mail can be sent to a name even when its chunk is not loaded. */
public final class MailRegistry extends SavedData {
	public record Entry(String name, UUID owner, String ownerName, String dimension, long pos) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("name").forGetter(Entry::name),
				UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner),
				Codec.STRING.fieldOf("owner_name").forGetter(Entry::ownerName),
				Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
				Codec.LONG.fieldOf("pos").forGetter(Entry::pos)).apply(i, Entry::new));
	}

	private static final Codec<MailRegistry> CODEC = Entry.CODEC.listOf().fieldOf("mailboxes").codec().xmap(MailRegistry::new, registry -> registry.entries);
	private static final SavedDataType<MailRegistry> TYPE = new SavedDataType<>(EndRealms.id("phase_mail"), MailRegistry::new, CODEC, null);

	private final List<Entry> entries;

	private MailRegistry() {
		this.entries = new ArrayList<>();
	}

	private MailRegistry(List<Entry> entries) {
		this.entries = new ArrayList<>(entries);
	}

	public static MailRegistry get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private static String key(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}

	public Entry find(String name) {
		for (Entry entry : entries) {
			if (key(entry.name()).equals(key(name))) {
				return entry;
			}
		}
		return null;
	}

	public Entry at(String dimension, long pos) {
		for (Entry entry : entries) {
			if (entry.dimension().equals(dimension) && entry.pos() == pos) {
				return entry;
			}
		}
		return null;
	}

	/** Adds or replaces the mailbox at the entry's position. */
	public void put(Entry entry) {
		entries.removeIf(old -> old.dimension().equals(entry.dimension()) && old.pos() == entry.pos());
		entries.add(entry);
		setDirty();
	}

	public void remove(String dimension, long pos) {
		if (entries.removeIf(old -> old.dimension().equals(dimension) && old.pos() == pos)) {
			setDirty();
		}
	}

	public List<Entry> all() {
		List<Entry> sorted = new ArrayList<>(entries);
		sorted.sort(Comparator.comparing(entry -> key(entry.name())));
		return sorted;
	}
}
