package hu.endrealms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import hu.endrealms.EndRealms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * /name &lt;name&gt; &lt;message&gt;: for admins only. Puts a line in everyone's chat exactly as if that player had written it
 * ("&lt;name&gt; message"). The name need not be online; if it is, their display name (with its colours) is used. Every use
 * is written to the server log with who really sent it.
 */
public final class NameCommand {
	private NameCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("name")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("nev", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder))
						.then(Commands.argument("uzenet", StringArgumentType.greedyString())
								.executes(ctx -> say(ctx.getSource(), StringArgumentType.getString(ctx, "nev"), StringArgumentType.getString(ctx, "uzenet"))))));
	}

	private static int say(CommandSourceStack source, String name, String message) {
		MinecraftServer server = source.getServer();
		ServerPlayer player = server.getPlayerList().getPlayerByName(name);
		Component who = player != null ? player.getDisplayName() : Component.literal(name);
		server.getPlayerList().broadcastSystemMessage(Component.translatable("chat.type.text", who, Component.literal(message)), false);
		EndRealms.LOGGER.info("[/name] {} wrote as {}: {}", source.getTextName(), name, message);
		return 1;
	}
}
