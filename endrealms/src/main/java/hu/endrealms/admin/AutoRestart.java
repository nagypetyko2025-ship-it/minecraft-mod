package hu.endrealms.admin;

import hu.endrealms.EndRealms;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Restarts go through start-server.bat, which loops: when the server exits and restart.flag is there, it starts again
 * at once, installing the mods waiting in mods-update on the way.
 * <p>
 * While the bat gets the server going (backup, mods), starting.flag tells the control panel app that it is on its way
 * rather than down; the server removes it once it is up.
 * <p>
 * And when a new version waits in mods-update and nobody has been online for {@link #EMPTY_MILLIS}, the server restarts
 * by itself to put it in place. This is watched by the clock on a thread of its own: an empty server pauses after a
 * minute (pause-when-empty-seconds) and stops ticking, so a tick counter would never get there.
 */
public final class AutoRestart {
	public static final String RESTART_FLAG = "restart.flag";
	public static final String STARTING_FLAG = "starting.flag";
	static final long EMPTY_MILLIS = 2 * 60 * 1000L;
	private static final long CHECK_SECONDS = 10;

	private static volatile long emptySince;
	private static volatile boolean restarting;
	private static java.util.concurrent.ScheduledExecutorService watcher;

	private AutoRestart() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				Files.deleteIfExists(server.getServerDirectory().resolve(STARTING_FLAG));
			} catch (IOException ignored) {
			}
			restarting = false;
			emptySince = System.currentTimeMillis();
			watcher = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(runnable -> {
				Thread thread = new Thread(runnable, "EndRealms-AutoRestart");
				thread.setDaemon(true);
				return thread;
			});
			watcher.scheduleWithFixedDelay(() -> check(server), CHECK_SECONDS, CHECK_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (watcher != null) {
				watcher.shutdownNow();
				watcher = null;
			}
		});
	}

	/** Stops the server so that start-server.bat starts it again straight away. */
	static void restart(MinecraftServer server, String why) {
		if (restarting) {
			return;
		}
		restarting = true;
		EndRealms.LOGGER.info("Restarting: {}", why);
		try {
			Files.writeString(server.getServerDirectory().resolve(RESTART_FLAG), why);
		} catch (IOException e) {
			EndRealms.LOGGER.error("Could not write {}", RESTART_FLAG, e);
		}
		server.halt(false);
	}

	private static void check(MinecraftServer server) {
		try {
			if (restarting || !server.isRunning()) {
				return;
			}
			long now = System.currentTimeMillis();
			if (server.getPlayerCount() > 0) {
				emptySince = now;
				return;
			}
			if (now - emptySince >= EMPTY_MILLIS && updateWaiting(server.getServerDirectory().resolve("mods-update"))) {
				server.execute(() -> restart(server, "an update waits in mods-update and nobody has been online for two minutes"));
			}
		} catch (RuntimeException e) {
			EndRealms.LOGGER.warn("Auto-restart check failed", e);
		}
	}

	static boolean updateWaiting(Path folder) {
		if (!Files.isDirectory(folder)) {
			return false;
		}
		try (DirectoryStream<Path> jars = Files.newDirectoryStream(folder, "*.jar")) {
			return jars.iterator().hasNext();
		} catch (IOException e) {
			return false;
		}
	}
}
