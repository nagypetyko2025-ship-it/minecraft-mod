package hu.endrealms.net;

import hu.endrealms.EndRealms;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * Asks every joining client for its End Realms version while it is still logging in, before the registries are synced,
 * and turns away anyone whose version differs with a plain message instead of a "network protocol error" later on.
 * Clients older than this check (or without the mod) cannot answer at all; they are told to install the new pack too.
 * The text is plain Hungarian, not a translation key: a client without the matching mod has no way to translate it.
 */
public final class VersionCheck {
	public static final Identifier ID = EndRealms.id("version");

	private VersionCheck() {
	}

	public static String version() {
		return FabricLoader.getInstance().getModContainer(EndRealms.MOD_ID).map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("?");
	}

	public static FriendlyByteBuf versionBuf() {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeUtf(version());
		return buf;
	}

	public static void init() {
		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> sender.sendPacket(ID, versionBuf()));
		ServerLoginNetworking.registerGlobalReceiver(ID, (server, handler, understood, buf, synchronizer, responder) -> {
			String theirs = understood ? buf.readUtf(64) : null;
			boolean updating = understood && buf.isReadable() && buf.readBoolean();     // 0.40.0 and later fetch the update themselves
			String ours = version();
			if (!ours.equals(theirs)) {
				EndRealms.LOGGER.info("Turned away a client with End Realms {} (server runs {}){}", theirs == null ? "missing or older than 0.38.0" : theirs, ours,
						updating ? ", it is downloading the update" : "");
				handler.disconnect(message(ours, theirs, updating));
			}
		});
	}

	static Component message(String server, String client, boolean updating) {
		boolean clientNewer = client != null && compare(client, server) > 0;
		MutableComponent text = Component.empty();
		text.append(Component.literal("End Realms – eltérő verzió\n\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.append(Component.literal("A szerveren: ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(server + "\n").withStyle(ChatFormatting.GREEN))
				.append(Component.literal("Nálad: ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal((client == null ? "régebbi, vagy nincs telepítve" : client) + "\n\n").withStyle(ChatFormatting.RED));
		if (updating) {
			text.append(Component.literal("A játék most letölti az új verziót.\nHa a jobb felső sarokban megjelenik, hogy kész,\nzárd be a Minecraftot, indítsd újra, és lépj be újra.").withStyle(ChatFormatting.WHITE));
		} else if (clientNewer) {
			text.append(Component.literal("Neked már újabb verziód van, mint a szervernek.\nVárj, amíg a szervert frissítik, aztán lépj be újra.").withStyle(ChatFormatting.WHITE));
		} else {
			text.append(Component.literal("Telepítsd a legújabb játékos-csomagot (jatekos-csomag.zip),\nindítsd újra a játékot, és lépj be újra.").withStyle(ChatFormatting.WHITE));
		}
		return text;
	}

	/** Compares dotted versions number by number ("0.38.0" > "0.37.12"); anything that is not a number counts as 0. */
	public static int compare(String a, String b) {
		String[] x = a.split("[.+-]");
		String[] y = b.split("[.+-]");
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int c = Integer.compare(number(x, i), number(y, i));
			if (c != 0) {
				return c;
			}
		}
		return 0;
	}

	private static int number(String[] parts, int i) {
		try {
			return i < parts.length ? Integer.parseInt(parts[i]) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
