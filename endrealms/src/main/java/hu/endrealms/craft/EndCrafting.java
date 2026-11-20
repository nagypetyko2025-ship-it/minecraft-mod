package hu.endrealms.craft;

import hu.endrealms.EndRealms;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * The End Crafting Table's recipes: their type and serializer, and the list the table's window shows. Recipes are not
 * sent to clients by the game, so the server sends each player that list itself, on joining and after /reload.
 */
public final class EndCrafting {
	public static final RecipeType<EndCraftingRecipe> TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE, EndRealms.id("end_crafting"), new RecipeType<>() {
		@Override
		public String toString() {
			return "endrealms:end_crafting";
		}
	});
	public static final RecipeSerializer<EndCraftingRecipe> SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, EndRealms.id("end_crafting"),
			new RecipeSerializer<>(EndCraftingRecipe.MAP_CODEC, EndCraftingRecipe.STREAM_CODEC));

	/** One recipe as the window draws it: the result and, for each of the 25 cells, the item ids that fit there. */
	public record Entry(ItemStack result, List<String> cells) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
				ItemStack.STREAM_CODEC, Entry::result,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Entry::cells,
				Entry::new);
	}

	public record Recipes(List<Entry> entries) implements CustomPacketPayload {
		public static final Type<Recipes> TYPE = new Type<>(EndRealms.id("end_crafting_recipes"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Recipes> CODEC = Entry.CODEC.apply(ByteBufCodecs.list()).map(Recipes::new, Recipes::entries);

		@Override
		public Type<Recipes> type() {
			return TYPE;
		}
	}

	private EndCrafting() {
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Recipes.TYPE, Recipes.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player, server));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> server.getPlayerList().getPlayers().forEach(player -> send(player, server)));
	}

	/** Every End crafting recipe, in a fixed order that the window's buttons refer to by number. */
	public static List<RecipeHolder<EndCraftingRecipe>> recipes(MinecraftServer server) {
		List<RecipeHolder<EndCraftingRecipe>> list = new ArrayList<>();
		for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
			if (holder.value() instanceof EndCraftingRecipe) {
				@SuppressWarnings("unchecked")
				RecipeHolder<EndCraftingRecipe> recipe = (RecipeHolder<EndCraftingRecipe>) holder;
				list.add(recipe);
			}
		}
		list.sort(Comparator.comparing(holder -> holder.id().identifier().toString()));
		return list;
	}

	private static void send(ServerPlayer player, MinecraftServer server) {
		if (!ServerPlayNetworking.canSend(player, Recipes.TYPE)) {
			return;
		}
		List<Entry> entries = new ArrayList<>();
		for (RecipeHolder<EndCraftingRecipe> holder : recipes(server)) {
			List<String> cells = new ArrayList<>();
			for (Optional<Ingredient> cell : holder.value().grid()) {
				cells.add(cell.map(ingredient -> ingredient.items().map(Holder::value).map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
						.collect(Collectors.joining(","))).orElse(""));
			}
			entries.add(new Entry(holder.value().result().create(), cells));
		}
		ServerPlayNetworking.send(player, new Recipes(entries));
	}
}
