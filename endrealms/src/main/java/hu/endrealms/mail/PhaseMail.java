package hu.endrealms.mail;

import hu.endrealms.EndRealms;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Phase Mail: mailboxes are found by name in {@link MailRegistry}. Sending moves the stack in the send slot straight
 * into the named mailbox, loading its chunk if needed. Only a mailbox's owner can rename it.
 */
public final class PhaseMail {
	private static final int MAX_NAME = 24;

	public record Rename(BlockPos pos, String name) implements CustomPacketPayload {
		public static final Type<Rename> TYPE = new Type<>(EndRealms.id("mail_rename"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Rename> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC.cast(), Rename::pos, ByteBufCodecs.STRING_UTF8.cast(), Rename::name, Rename::new);

		@Override
		public Type<Rename> type() {
			return TYPE;
		}
	}

	public record Send(BlockPos pos, String target) implements CustomPacketPayload {
		public static final Type<Send> TYPE = new Type<>(EndRealms.id("mail_send"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Send> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC.cast(), Send::pos, ByteBufCodecs.STRING_UTF8.cast(), Send::target, Send::new);

		@Override
		public Type<Send> type() {
			return TYPE;
		}
	}

	/** What the open mail window shows: this mailbox, whether the viewer owns it, every mailbox, and the last result. */
	public record State(BlockPos pos, String name, boolean owner, List<String> names, List<String> owners, String message) implements CustomPacketPayload {
		public static final Type<State> TYPE = new Type<>(EndRealms.id("mail_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC.cast(), State::pos,
				ByteBufCodecs.STRING_UTF8.cast(), State::name,
				ByteBufCodecs.BOOL.cast(), State::owner,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).cast(), State::names,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).cast(), State::owners,
				ByteBufCodecs.STRING_UTF8.cast(), State::message,
				State::new);

		@Override
		public Type<State> type() {
			return TYPE;
		}
	}

	private PhaseMail() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Rename.TYPE, Rename.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Send.TYPE, Send.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Rename.TYPE, (payload, context) -> context.server().execute(() -> rename(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(Send.TYPE, (payload, context) -> context.server().execute(() -> send(context.player(), payload)));
	}

	private static String dimension(Level level) {
		return level.dimension().identifier().toString();
	}

	private static PhaseMailboxBlockEntity open(ServerPlayer player, BlockPos pos) {
		if (player.containerMenu instanceof PhaseMailboxMenu menu && menu.pos().equals(pos)
				&& player.level().getBlockEntity(pos) instanceof PhaseMailboxBlockEntity mailbox) {
			return mailbox;
		}
		return null;
	}

	private static boolean valid(String name) {
		String trimmed = name.trim();
		return !trimmed.isEmpty() && trimmed.length() <= MAX_NAME && trimmed.chars().noneMatch(c -> c < 32 || c == '§');
	}

	/** A new mailbox gets its item's anvil name, or "<player> postája", made unique with a number. */
	static void claim(ServerPlayer player, PhaseMailboxBlockEntity mailbox, ItemStack stack) {
		MailRegistry registry = MailRegistry.get(player.level().getServer());
		Component custom = stack.get(DataComponents.CUSTOM_NAME);
		// the server has no language of its own (it would say "'s Mailbox"), and the players of this server speak Hungarian
		String base = custom != null && valid(custom.getString()) ? custom.getString().trim() : player.getName().getString() + " postája";
		String name = base;
		for (int n = 2; registry.find(name) != null; n++) {
			name = base + " " + n;
		}
		mailbox.setIdentity(name, player.getUUID(), player.getName().getString());
		registry.put(new MailRegistry.Entry(name, player.getUUID(), player.getName().getString(), dimension(mailbox.getLevel()), mailbox.getBlockPos().asLong()));
	}

	static void unregister(PhaseMailboxBlockEntity mailbox) {
		if (mailbox.getLevel() instanceof ServerLevel level) {
			MailRegistry.get(level.getServer()).remove(dimension(level), mailbox.getBlockPos().asLong());
		}
	}

	private static void rename(ServerPlayer player, Rename payload) {
		PhaseMailboxBlockEntity mailbox = open(player, payload.pos());
		if (mailbox == null) {
			return;
		}
		String message;
		MailRegistry registry = MailRegistry.get(player.level().getServer());
		String name = payload.name().trim();
		MailRegistry.Entry taken = registry.find(name);
		if (mailbox.owner() != null && !mailbox.owner().equals(player.getUUID())) {
			message = "mail.endrealms.not_owner";
		} else if (!valid(name)) {
			message = "mail.endrealms.bad_name";
		} else if (taken != null && !(taken.dimension().equals(dimension(player.level())) && taken.pos() == payload.pos().asLong())) {
			message = "mail.endrealms.name_taken";
		} else {
			mailbox.setIdentity(name, player.getUUID(), player.getName().getString());
			registry.put(new MailRegistry.Entry(name, player.getUUID(), player.getName().getString(), dimension(player.level()), payload.pos().asLong()));
			message = "mail.endrealms.renamed";
		}
		sendState(player, message);
	}

	private static void send(ServerPlayer player, Send payload) {
		PhaseMailboxBlockEntity from = open(player, payload.pos());
		if (from == null || !(player.containerMenu instanceof PhaseMailboxMenu menu)) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		MailRegistry registry = MailRegistry.get(server);
		ItemStack stack = menu.outgoing().getItem(0);
		MailRegistry.Entry target = registry.find(payload.target());
		String message;
		if (stack.isEmpty()) {
			message = "mail.endrealms.nothing";
		} else if (target == null) {
			message = "mail.endrealms.no_such";
		} else if (target.dimension().equals(dimension(player.level())) && target.pos() == payload.pos().asLong()) {
			message = "mail.endrealms.self";
		} else {
			ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(target.dimension())));
			BlockPos pos = BlockPos.of(target.pos());
			if (level != null) {
				level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
			}
			if (level == null || !(level.getBlockEntity(pos) instanceof PhaseMailboxBlockEntity to)) {
				registry.remove(target.dimension(), target.pos());
				message = "mail.endrealms.no_such";
			} else {
				String what = stack.getHoverName().getString();
				int moved = to.deliver(stack);
				menu.outgoing().setChanged();
				if (moved == 0) {
					message = "mail.endrealms.full";
				} else {
					player.level().playSound(null, payload.pos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6F, 1.4F);
					level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6F, 1.4F);
					ServerPlayer recipient = server.getPlayerList().getPlayer(target.owner());
					if (recipient != null && recipient != player) {
						recipient.sendSystemMessage(Component.translatable("mail.endrealms.arrived", player.getName(), moved, what, target.name()));
					}
					message = stack.isEmpty() ? "mail.endrealms.sent" : "mail.endrealms.partly";
				}
			}
		}
		sendState(player, message);
	}

	/** Sends the open mail window its contents; message is a translation key, or empty. */
	public static void sendState(ServerPlayer player, String message) {
		if (!(player.containerMenu instanceof PhaseMailboxMenu menu) || !(player.level().getBlockEntity(menu.pos()) instanceof PhaseMailboxBlockEntity mailbox)) {
			return;
		}
		List<String> names = new ArrayList<>();
		List<String> owners = new ArrayList<>();
		for (MailRegistry.Entry entry : MailRegistry.get(player.level().getServer()).all()) {
			names.add(entry.name());
			owners.add(entry.ownerName());
		}
		boolean owner = mailbox.owner() == null || mailbox.owner().equals(player.getUUID());
		ServerPlayNetworking.send(player, new State(menu.pos(), mailbox.name(), owner, names, owners, message));
	}
}
