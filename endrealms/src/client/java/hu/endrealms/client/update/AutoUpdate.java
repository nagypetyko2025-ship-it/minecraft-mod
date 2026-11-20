package hu.endrealms.client.update;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Fetches the End Realms version the server runs from the mod's GitHub repository and puts it in place of this one when
 * the game closes.
 * <p>
 * The running jar cannot be replaced while the game has it open (Windows locks it), so the new jar is saved next to it as
 * {@code endrealms-X.jar.update}, which Fabric ignores. When the game exits, a small script waits until the old jar can
 * be deleted, deletes it and renames the update to {@code endrealms-X.jar}. If anything goes wrong on the way, the old jar
 * simply stays: there are never two End Realms jars in the mods folder at once.
 * <p>
 * This class knows nothing about Minecraft, so it can be tested on its own.
 */
public final class AutoUpdate {
	public static final String BASE = "https://raw.githubusercontent.com/nagypetyko2025-ship-it/minecraft-mod/main/updates/";
	/** Only plain dotted numbers: the version goes into a URL, file names and a script. */
	private static final Pattern VERSION = Pattern.compile("\\d{1,4}(\\.\\d{1,4}){1,3}");
	private static final long MAX_SIZE = 64L << 20;

	private static CompletableFuture<Boolean> running;
	private static String runningVersion;
	private static boolean hookAdded;

	private AutoUpdate() {
	}

	public static boolean validVersion(String version) {
		return version != null && VERSION.matcher(version).matches();
	}

	/**
	 * Starts downloading that version into the folder of {@code ownJar}, unless it is already being fetched.
	 * {@code done} is told whether the download worked. Returns false when there is nothing it can do.
	 */
	public static synchronized boolean start(String version, Path ownJar, String base, Consumer<Boolean> done) {
		if (!validVersion(version) || ownJar == null || !Files.isRegularFile(ownJar) || !ownJar.toString().endsWith(".jar")) {
			return false;
		}
		if (running != null && version.equals(runningVersion) && !(running.isDone() && !running.join())) {
			return true;    // already fetching (or fetched) it
		}
		Path dir = ownJar.getParent();
		Path update = dir.resolve("endrealms-" + version + ".jar.update");
		Path target = dir.resolve("endrealms-" + version + ".jar");
		runningVersion = version;
		running = CompletableFuture.supplyAsync(() -> {
			try {
				download(base + "endrealms-" + version + ".jar", version, update);
				return true;
			} catch (Exception e) {
				System.err.println("[End Realms] Could not download version " + version + ": " + e);
				return false;
			}
		});
		running.thenAccept(done);
		if (!hookAdded) {
			hookAdded = true;
			Runtime.getRuntime().addShutdownHook(new Thread(AutoUpdate::onExit, "End Realms updater"));
		}
		pendingOld = ownJar;
		pendingUpdate = update;
		pendingTarget = target;
		return true;
	}

	private static volatile Path pendingOld;
	private static volatile Path pendingUpdate;
	private static volatile Path pendingTarget;

	/** Downloads the jar, checks that it really is End Realms of that version, and only then saves it. */
	static void download(String url, String version, Path update) throws IOException, InterruptedException {
		HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120)).header("User-Agent", "EndRealms-Updater").GET().build();
		HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
		if (response.statusCode() != 200) {
			throw new IOException("HTTP " + response.statusCode() + " for " + url);
		}
		byte[] jar = response.body();
		if (jar.length > MAX_SIZE) {
			throw new IOException("too large: " + jar.length);
		}
		String found = modVersion(new ByteArrayInputStream(jar));
		if (!version.equals(found)) {
			throw new IOException("the downloaded jar is End Realms " + found + ", not " + version);
		}
		Path part = update.resolveSibling(update.getFileName() + ".part");
		Files.write(part, jar);
		Files.move(part, update, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	/** The version in the jar's fabric.mod.json if it is End Realms, otherwise null. */
	static String modVersion(InputStream jar) throws IOException {
		try (ZipInputStream zip = new ZipInputStream(jar)) {
			for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
				if (entry.getName().equals("fabric.mod.json")) {
					String json = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
					if (!Pattern.compile("\"id\"\\s*:\\s*\"endrealms\"").matcher(json).find()) {
						return null;
					}
					Matcher m = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
					return m.find() ? m.group(1) : null;
				}
			}
		}
		return null;
	}

	/** Leftovers of an earlier update that never got put in place. */
	public static void cleanUp(Path ownJar) {
		if (ownJar == null || ownJar.getParent() == null) {
			return;
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(ownJar.getParent(), "endrealms-*.jar.update*")) {
			for (Path file : files) {
				Files.deleteIfExists(file);
			}
		} catch (IOException ignored) {
		}
	}

	private static void onExit() {
		CompletableFuture<Boolean> job = running;
		try {
			if (job == null || !job.get(60, TimeUnit.SECONDS)) {
				return;
			}
		} catch (Exception e) {
			return;
		}
		try {
			swap(pendingOld, pendingUpdate, pendingTarget);
		} catch (Exception e) {
			System.err.println("[End Realms] Could not put the update in place: " + e);
		}
	}

	/** Deletes the old jar once the game has let go of it, then renames the update into its place. */
	static void swap(Path old, Path update, Path target) throws IOException {
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
			Files.deleteIfExists(old);       // elsewhere an open file can be deleted
			Files.move(update, target, StandardCopyOption.REPLACE_EXISTING);
			return;
		}
		Path script = Files.createTempFile("endrealms-update-", ".cmd");
		Files.writeString(script, String.join("\r\n",
				"@echo off",
				"set n=0",
				":retry",
				"del /f /q \"%~1\" >nul 2>&1",
				"if not exist \"%~1\" goto swap",
				"set /a n+=1",
				"if %n% geq 90 goto end",
				"ping -n 2 127.0.0.1 >nul",
				"goto retry",
				":swap",
				"move /y \"%~2\" \"%~3\" >nul",
				":end",
				"del /f /q \"%~f0\" >nul 2>&1",
				""), StandardCharsets.US_ASCII);
		new ProcessBuilder("cmd.exe", "/c", script.toString(), old.toString(), update.toString(), target.toString())
				.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
	}
}
