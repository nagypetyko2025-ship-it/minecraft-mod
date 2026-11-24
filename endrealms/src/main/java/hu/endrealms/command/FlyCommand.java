package hu.endrealms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * /fly: switches flying on or off for yourself, in survival too, like creative flight (double-tap jump). /fly &lt;player&gt;
 * does it for someone else. Operators only by default; a rank can open it to its members in the control panel.
 * Switching it off in mid-air gives a few seconds of slow falling, so nobody drops to their death. Changing game mode
 * resets it, as the game does.
 */
public final class FlyCommand {
	private FlyCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("fly")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> toggle(ctx.getSource(), ctx.getSource().getPlayerOrException()))
				.then(Commands.argument("jatekos", EntityArgument.player())
						.executes(ctx -> toggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "jatekos")))));
	}

	private static int toggle(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
		if (player.isCreative() || player.isSpectator()) {
			source.sendFailure(Component.literal(player.getName().getString() + " kreatív vagy szemlélő módban van, ott amúgy is repülhet."));
			return 0;
		}
		boolean on = !player.getAbilities().mayfly;
		player.getAbilities().mayfly = on;
		if (!on) {
			if (player.getAbilities().flying && !player.onGround()) {
				player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200));
			}
			player.getAbilities().flying = false;
		}
		player.onUpdateAbilities();
		Component state = Component.literal(on ? "bekapcsolva" : "kikapcsolva").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
		player.sendSystemMessage(Component.literal("Repülés ").append(state).append(on ? Component.literal(" (dupla ugrással indul)") : Component.empty()));
		if (source.getPlayer() != player) {
			source.sendSuccess(() -> Component.literal(player.getName().getString() + " repülése ").append(state), true);
		}
		return 1;
	}
}
