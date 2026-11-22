package hu.endrealms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;
import hu.endrealms.EndRealms;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

/** /astra locate &lt;name&gt; - finds the nearest End Realms structure (void_bastion, end_village, rift_sanctum, sky_wreck, observatory, crypt, void_citadel) or biome (astral_forest, astral_basin, prism_wastes, scorched_barrens). */
public final class AstraCommand {
	/** Structure names offered by the command (ids in the endrealms namespace). */
	private static final List<String> STRUCTURES = List.of("void_bastion", "end_village", "rift_sanctum", "sky_wreck", "observatory", "crypt", "void_citadel");
	/** Biome names offered by the command (ids in the endrealms namespace). */
	private static final List<String> BIOMES = List.of("astral_forest", "astral_basin", "prism_wastes", "scorched_barrens", "dragon_graveyard",
			"sculk_barrens", "withered_woods");

	private AstraCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("astra")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("locate")
						.then(Commands.argument("structure", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										java.util.stream.Stream.concat(STRUCTURES.stream(), BIOMES.stream()), builder))
								.executes(AstraCommand::locate)))
				// debug: a Sculk Portal right under your feet
				.then(Commands.literal("sculk")
						.then(Commands.literal("portal:summon")
								.executes(AstraCommand::summonSculkPortal))));
	}

	private static int summonSculkPortal(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
		hu.endrealms.sculk.SculkDimension.summonPortal(player.level(), player.blockPosition());
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.endrealms.astra.sculk_portal"), true);
		return 1;
	}

	private static int locate(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		String name = StringArgumentType.getString(ctx, "structure");
		if (BIOMES.contains(name)) {
			return locateBiome(source, name);
		}
		Component display = Component.translatable("structure.endrealms." + name);
		if (!STRUCTURES.contains(name)) {
			source.sendFailure(Component.translatable("commands.endrealms.astra.locate.unknown", name));
			return 0;
		}
		ServerLevel level = source.getLevel();
		ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, EndRealms.id(name));
		Optional<Holder.Reference<Structure>> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(key);
		if (holder.isEmpty()) {
			source.sendFailure(Component.translatable("commands.endrealms.astra.locate.unknown", name));
			return 0;
		}
		BlockPos from = BlockPos.containing(source.getPosition());
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, HolderSet.direct(holder.get()), from, 100, false);
		if (found == null) {
			source.sendFailure(Component.translatable("commands.endrealms.astra.locate.not_found", display));
			return 0;
		}
		return report(source, display, from, found.getFirst());
	}

	private static int locateBiome(CommandSourceStack source, String name) {
		Component display = Component.translatable("biome.endrealms." + name);
		ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, EndRealms.id(name));
		BlockPos from = BlockPos.containing(source.getPosition());
		Pair<BlockPos, Holder<Biome>> found = source.getLevel().findClosestBiome3d(biome -> biome.is(key), from, 6400, 32, 64);
		if (found == null) {
			source.sendFailure(Component.translatable("commands.endrealms.astra.locate.not_found", display));
			return 0;
		}
		return report(source, display, from, found.getFirst());
	}

	private static int report(CommandSourceStack source, Component display, BlockPos from, BlockPos pos) {
		int distance = Mth.floor(Mth.sqrt((float) from.distSqr(new BlockPos(pos.getX(), from.getY(), pos.getZ()))));
		Component coords = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), "~", pos.getZ()))
				.withStyle(style -> style.withColor(ChatFormatting.GREEN)
						.withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + pos.getX() + " ~ " + pos.getZ()))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))));
		source.sendSuccess(() -> Component.translatable("commands.endrealms.astra.locate.success", display, coords, distance), false);
		return distance;
	}
}
