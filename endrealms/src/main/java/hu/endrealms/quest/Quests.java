package hu.endrealms.quest;

import com.mojang.serialization.Codec;
import hu.endrealms.EndRealms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Server side of the quest tree. Objectives are never counted here: they are read from the player's own advancements,
 * inventory and statistics, so quests also recognise everything done before they existed. Only the claimed rewards are stored.
 */
public final class Quests {
	public record Request() implements CustomPacketPayload {
		public static final Type<Request> TYPE = new Type<>(EndRealms.id("quest_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.unit(new Request());

		@Override
		public Type<Request> type() {
			return TYPE;
		}
	}

	public record Claim(String quest) implements CustomPacketPayload {
		public static final Type<Claim> TYPE = new Type<>(EndRealms.id("quest_claim"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Claim> CODEC = ByteBufCodecs.STRING_UTF8.<RegistryFriendlyByteBuf>cast().map(Claim::new, Claim::quest);

		@Override
		public Type<Claim> type() {
			return TYPE;
		}
	}

	/** progress: "questId#objectiveIndex" to the amount reached so far. */
	public record State(List<String> claimed, Map<String, Integer> progress) implements CustomPacketPayload {
		public static final Type<State> TYPE = new Type<>(EndRealms.id("quest_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), State::claimed,
				ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), s -> new HashMap<>(s.progress()),
				State::new);

		@Override
		public Type<State> type() {
			return TYPE;
		}
	}

	private static final AttachmentType<List<String>> CLAIMED = AttachmentRegistry.create(EndRealms.id("claimed_quests"),
			builder -> builder.persistent(Codec.STRING.listOf()).copyOnDeath().initializer(List::of));

	private Quests() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Claim.TYPE, Claim.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (payload, context) -> context.server().execute(() -> sendState(context.player())));
		ServerPlayNetworking.registerGlobalReceiver(Claim.TYPE, (payload, context) -> context.server().execute(() -> claim(context.player(), payload.quest())));
	}

	private static List<String> claimed(ServerPlayer player) {
		return player.getAttachedOrCreate(CLAIMED);
	}

	private static int progress(ServerPlayer player, QuestBook.Objective objective) {
		Identifier id = Identifier.parse(objective.id());
		switch (objective.type()) {
			case "advancement" -> {
				AdvancementHolder holder = player.level().getServer().getAdvancements().get(id);
				if (holder == null) {
					return 0;
				}
				AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
				if (objective.criterion() == null) {
					return progress.isDone() ? 1 : 0;
				}
				var criterion = progress.getCriterion(objective.criterion());
				return criterion != null && criterion.isDone() ? 1 : 0;
			}
			case "item" -> {
				// "Obtain" counts what is in the inventory now, but also what was ever crafted or picked up: a table or a
				// block placed right after crafting it would otherwise never count.
				Item item = BuiltInRegistries.ITEM.getValue(id);
				int count = 0;
				for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
					ItemStack stack = player.getInventory().getItem(slot);
					if (stack.is(item)) {
						count += stack.getCount();
					}
				}
				int crafted = player.getStats().getValue(Stats.ITEM_CRAFTED, item);
				int pickedUp = player.getStats().getValue(Stats.ITEM_PICKED_UP, item);
				return Math.max(count, Math.max(crafted, pickedUp));
			}
			case "kill" -> {
				return player.getStats().getValue(Stats.ENTITY_KILLED, BuiltInRegistries.ENTITY_TYPE.getValue(id));
			}
			case "mine" -> {
				return player.getStats().getValue(Stats.BLOCK_MINED, BuiltInRegistries.BLOCK.getValue(id));
			}
			default -> {
				return 0;
			}
		}
	}

	private static boolean finished(ServerPlayer player, QuestBook.Quest quest) {
		return quest.objectives().stream().allMatch(objective -> progress(player, objective) >= objective.count());
	}

	public static void sendState(ServerPlayer player) {
		Map<String, Integer> progress = new HashMap<>();
		for (QuestBook.Chapter chapter : QuestBook.chapters()) {
			for (QuestBook.Quest quest : QuestBook.inChapter(chapter.id())) {
				for (int i = 0; i < quest.objectives().size(); i++) {
					progress.put(quest.id() + "#" + i, progress(player, quest.objectives().get(i)));
				}
			}
		}
		ServerPlayNetworking.send(player, new State(claimed(player), progress));
	}

	private static void claim(ServerPlayer player, String id) {
		QuestBook.Quest quest = QuestBook.quest(id);
		List<String> done = claimed(player);
		if (quest == null || done.contains(id) || !done.containsAll(quest.requires()) || !finished(player, quest)) {
			sendState(player);
			return;
		}
		for (QuestBook.Reward reward : quest.rewards()) {
			ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(reward.item())), reward.count());
			if (!player.getInventory().add(stack) && !stack.isEmpty()) {
				player.spawnAtLocation(player.level(), stack);
			}
		}
		if (quest.xp() > 0) {
			player.giveExperiencePoints(quest.xp());
		}
		List<String> updated = new ArrayList<>(done);
		updated.add(id);
		player.setAttached(CLAIMED, List.copyOf(updated));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.3F);
		sendState(player);
	}
}
