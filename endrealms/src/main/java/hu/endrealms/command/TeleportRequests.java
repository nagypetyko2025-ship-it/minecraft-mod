package hu.endrealms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Teleport requests for everyone, no operator rights needed: /tpa asks, the other player answers with /tpaccept or /tpdeny.
 * Requests live in memory only and end after a minute, or when either player leaves.
 */
public final class TeleportRequests {
	private static final int LIFETIME_TICKS = 60 * 20;
	private static final int COOLDOWN_TICKS = 10 * 20;

	private record Request(UUID from, String fromName, UUID to, String toName, int expiresAt) {
	}

	private static final List<Request> REQUESTS = new ArrayList<>();
	private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

	private TeleportRequests() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(TeleportRequests::expire);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player, server));
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tpa")
				.then(Commands.argument("jatekos", EntityArgument.player()).executes(TeleportRequests::request)));
		dispatcher.register(Commands.literal("tpaccept")
				.executes(context -> answer(context, null, true))
				.then(Commands.argument("jatekos", EntityArgument.player())
						.executes(context -> answer(context, EntityArgument.getPlayer(context, "jatekos"), true))));
		dispatcher.register(Commands.literal("tpdeny")
				.executes(context -> answer(context, null, false))
				.then(Commands.argument("jatekos", EntityArgument.player())
						.executes(context -> answer(context, EntityArgument.getPlayer(context, "jatekos"), false))));
		dispatcher.register(Commands.literal("tpcancel").executes(TeleportRequests::cancel));
	}

	private static int request(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer from = context.getSource().getPlayerOrException();
		ServerPlayer to = EntityArgument.getPlayer(context, "jatekos");
		MinecraftServer server = context.getSource().getServer();
		if (from == to) {
			context.getSource().sendFailure(Component.literal("Magadhoz nem teleportálhatsz."));
			return 0;
		}
		int now = server.getTickCount();
		Integer last = LAST_SENT.get(from.getUUID());
		if (last != null && now - last < COOLDOWN_TICKS) {
			int seconds = (COOLDOWN_TICKS - (now - last) + 19) / 20;
			context.getSource().sendFailure(Component.literal("Várj még " + seconds + " másodpercet a következő kérés előtt."));
			return 0;
		}
		REQUESTS.removeIf(request -> request.from().equals(from.getUUID()));
		REQUESTS.add(new Request(from.getUUID(), from.getName().getString(), to.getUUID(), to.getName().getString(), now + LIFETIME_TICKS));
		LAST_SENT.put(from.getUUID(), now);

		String name = from.getName().getString();
		from.sendSystemMessage(info("Teleportkérés elküldve neki: " + to.getName().getString() + ". Egy perc múlva lejár. ")
				.append(button("[Visszavonás]", ChatFormatting.GRAY, "/tpcancel", "A kérés visszavonása")));
		to.sendSystemMessage(info(name + " szeretne hozzád teleportálni. ")
				.append(button("[Elfogadás]", ChatFormatting.GREEN, "/tpaccept " + name, name + " ide teleportál"))
				.append(Component.literal(" "))
				.append(button("[Elutasítás]", ChatFormatting.RED, "/tpdeny " + name, "A kérés elutasítása")));
		return 1;
	}

	private static int answer(CommandContext<CommandSourceStack> context, ServerPlayer named, boolean accept) throws CommandSyntaxException {
		ServerPlayer to = context.getSource().getPlayerOrException();
		Request found = null;
		for (Request request : REQUESTS) {
			if (request.to().equals(to.getUUID()) && (named == null || request.from().equals(named.getUUID()))) {
				found = request;
			}
		}
		if (found == null) {
			context.getSource().sendFailure(Component.literal(named == null ? "Nincs függőben lévő teleportkérésed." : named.getName().getString() + " nem küldött neked teleportkérést."));
			return 0;
		}
		REQUESTS.remove(found);
		ServerPlayer from = context.getSource().getServer().getPlayerList().getPlayer(found.from());
		if (from == null) {
			context.getSource().sendFailure(Component.literal(found.fromName() + " már nincs a szerveren."));
			return 0;
		}
		if (!accept) {
			from.sendSystemMessage(info(to.getName().getString() + " elutasította a teleportkérésedet."));
			to.sendSystemMessage(info("Elutasítottad " + found.fromName() + " kérését."));
			return 1;
		}
		if (!from.isAlive() || !to.isAlive()) {
			context.getSource().sendFailure(Component.literal("A teleportálás most nem lehetséges, mert valamelyikőtök halott."));
			from.sendSystemMessage(info("A teleportálás elmaradt, mert valamelyikőtök halott."));
			return 0;
		}
		if (from.isSleeping()) {
			from.stopSleepInBed(true, true);
		}
		from.stopRiding();
		from.teleportTo((ServerLevel) to.level(), to.getX(), to.getY(), to.getZ(), Set.of(), to.getYRot(), to.getXRot(), false);
		from.resetFallDistance();
		from.sendSystemMessage(info("Odateleportáltál hozzá: " + to.getName().getString() + "."));
		to.sendSystemMessage(info(found.fromName() + " hozzád teleportált."));
		return 1;
	}

	private static int cancel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer from = context.getSource().getPlayerOrException();
		MinecraftServer server = context.getSource().getServer();
		Request own = REQUESTS.stream().filter(request -> request.from().equals(from.getUUID())).findFirst().orElse(null);
		if (own == null) {
			context.getSource().sendFailure(Component.literal("Nincs elküldött teleportkérésed."));
			return 0;
		}
		REQUESTS.remove(own);
		from.sendSystemMessage(info("Visszavontad a teleportkérésedet."));
		tell(server, own.to(), own.fromName() + " visszavonta a teleportkérését.");
		return 1;
	}

	private static void expire(MinecraftServer server) {
		int now = server.getTickCount();
		if (now % 20 != 0 || REQUESTS.isEmpty()) {
			return;
		}
		List<Request> ended = REQUESTS.stream().filter(request -> now >= request.expiresAt()).toList();
		REQUESTS.removeAll(ended);
		for (Request request : ended) {
			tell(server, request.from(), request.toName() + " nem válaszolt, a teleportkérésed lejárt.");
			tell(server, request.to(), request.fromName() + " teleportkérése lejárt.");
		}
	}

	private static void forget(ServerPlayer player, MinecraftServer server) {
		UUID id = player.getUUID();
		String name = player.getName().getString();
		List<Request> ended = REQUESTS.stream().filter(request -> request.from().equals(id) || request.to().equals(id)).toList();
		REQUESTS.removeAll(ended);
		LAST_SENT.remove(id);
		for (Request request : ended) {
			tell(server, request.from().equals(id) ? request.to() : request.from(), name + " kilépett, a teleportkérés megszűnt.");
		}
	}

	private static void tell(MinecraftServer server, UUID player, String text) {
		ServerPlayer target = server.getPlayerList().getPlayer(player);
		if (target != null) {
			target.sendSystemMessage(info(text));
		}
	}

	private static MutableComponent info(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GOLD);
	}

	private static MutableComponent button(String label, ChatFormatting color, String command, String hover) {
		return Component.literal(label).withStyle(style -> style.withColor(color).withBold(true)
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}
}
