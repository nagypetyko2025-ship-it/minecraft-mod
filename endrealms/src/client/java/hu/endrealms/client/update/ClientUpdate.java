package hu.endrealms.client.update;

import hu.endrealms.EndRealms;
import hu.endrealms.net.VersionCheck;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/**
 * Answers the server's version question while logging in. When the server runs a newer End Realms, the client starts
 * fetching that version itself (see {@link AutoUpdate}) and tells the server so, which then words its message
 * accordingly; a toast says when the download is ready and the game only needs a restart.
 */
public final class ClientUpdate {
	private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(10000L);

	private ClientUpdate() {
	}

	public static void init() {
		AutoUpdate.cleanUp(ownJar());
		ClientLoginNetworking.registerGlobalReceiver(VersionCheck.ID, (client, handler, buf, listeners) -> {
			String server = buf.isReadable() ? buf.readUtf(64) : "";
			FriendlyByteBuf answer = VersionCheck.versionBuf();
			answer.writeBoolean(fetch(server));
			return CompletableFuture.completedFuture(answer);
		});
	}

	/** The jar End Realms was loaded from, or null when it was not a jar (in the development environment). */
	static Path ownJar() {
		try {
			return FabricLoader.getInstance().getModContainer(EndRealms.MOD_ID)
					.flatMap(mod -> mod.getOrigin().getPaths().stream().findFirst())
					.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".jar"))
					.orElse(null);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Starts fetching the server's version if it is newer than this one; returns whether it did. */
	private static boolean fetch(String server) {
		String ours = VersionCheck.version();
		if (!AutoUpdate.validVersion(server) || VersionCheck.compare(server, ours) <= 0) {
			return false;
		}
		boolean started = AutoUpdate.start(server, ownJar(), AutoUpdate.BASE, ok -> Minecraft.getInstance().execute(() -> toast(server, ok)));
		if (started) {
			EndRealms.LOGGER.info("The server runs End Realms {}, this game {}: downloading the update", server, ours);
		}
		return started;
	}

	private static void toast(String version, boolean ok) {
		Minecraft minecraft = Minecraft.getInstance();
		if (ok) {
			SystemToast.addOrUpdate(minecraft.gui.toastManager(), TOAST, Component.translatable("update.endrealms.ready", version),
					Component.translatable("update.endrealms.restart"));
		} else {
			SystemToast.addOrUpdate(minecraft.gui.toastManager(), TOAST, Component.translatable("update.endrealms.failed"),
					Component.translatable("update.endrealms.manual"));
		}
	}
}
