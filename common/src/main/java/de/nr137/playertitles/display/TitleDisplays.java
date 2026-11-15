package de.nr137.playertitles.display;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ChatTypeDecoration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import de.nr137.lib.lang.Translations;
import de.nr137.lib.text.TextWidth;
import de.nr137.playertitles.Config;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.mixin.DisplayAccessor;
import de.nr137.playertitles.mixin.PlayerInfoPacketAccessor;
import de.nr137.playertitles.mixin.TextDisplayAccessor;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleText;

/**
 * Showing worn titles: above the name (a text display riding the player, which exists only in the packets sent to
 * the players who see them: never added to the world, never saved), in the player list and in chat.
 */
public final class TitleDisplays {
	/**
	 * Just above the name tag. The display sits on the player's head (where passengers sit, at their height); a line
	 * of text is 0.225 high and grows upwards; the name tag hangs from 0.5 above the head down to 0.275 (EntityRenderer).
	 */
	public static final float HEIGHT = 0.55F;
	/** The wearer's own name for themselves (third person view), where their name tag is for others. */
	public static final float NAME_HEIGHT = 0.275F;

	/** Who sees whom (from the game's tracking), for every player, wearing a title or not. */
	private static final Map<UUID, Set<ServerPlayer>> VIEWERS = new HashMap<>();
	/** The display above each wearer, while they wear a title. */
	private static final Map<UUID, Display.TextDisplay> DISPLAYS = new HashMap<>();
	/**
	 * Each player's name, where their name tag is. The game shows no name tag on an entity that carries a passenger
	 * (LivingEntityRenderer), so while a player wears a title, the others see this one instead; the player themselves
	 * always gets it (the game never shows a player their own name tag), to see title and name in the third person view.
	 */
	private static final Map<UUID, Display.TextDisplay> OWN_NAMES = new HashMap<>();
	/** The player object and level each wearer was in at the last tick: after a respawn or a dimension change their
	 * client has a new world and needs their own displays again. */
	private static final Map<UUID, ServerPlayer> SELF_SENT = new HashMap<>();
	private static final Map<UUID, net.minecraft.world.level.Level> SELF_LEVEL = new HashMap<>();
	/** Each wearer's state at the last update: 0 shown, 1 sneaking (title hidden, name solid), 2 hidden. */
	private static final Map<UUID, Integer> HIDDEN = new HashMap<>();

	private TitleDisplays() {
	}

	// --- Above the head ---

	/** The game starts sending {@code wearer} to {@code viewer} (ServerEntityMixin). */
	public static void onSeen(ServerPlayer wearer, ServerPlayer viewer) {
		VIEWERS.computeIfAbsent(wearer.getUUID(), ignored -> new HashSet<>()).add(viewer);
		Display.TextDisplay display = DISPLAYS.get(wearer.getUUID());
		if (display != null) {
			spawnName(wearer, viewer);
			spawnFor(wearer, display, viewer);
		}
	}

	public static void onUnseen(ServerPlayer wearer, ServerPlayer viewer) {
		Set<ServerPlayer> viewers = VIEWERS.get(wearer.getUUID());
		if (viewers != null) {
			viewers.remove(viewer);
		}
		Display.TextDisplay display = DISPLAYS.get(wearer.getUUID());
		if (display != null) {
			viewer.connection.send(new ClientboundRemoveEntitiesPacket(display.getId(), ownNameId(wearer)));
		}
	}

	/** The display riding a player, for the passengers packet (SetPassengersPacketMixin); -1 if none. */
	public static int displayId(ServerPlayer player) {
		Display.TextDisplay display = DISPLAYS.get(player.getUUID());
		return display == null ? -1 : display.getId();
	}

	/** The display with the player's own name (only they know it; others skip the id); -1 if none. */
	public static int ownNameId(ServerPlayer player) {
		Display.TextDisplay display = OWN_NAMES.get(player.getUUID());
		return display == null ? -1 : display.getId();
	}

	/** What a player wears changed (or they joined): the display, the player list and everyone who sees them. */
	public static void refresh(ServerPlayer wearer) {
		Display.TextDisplay old = DISPLAYS.remove(wearer.getUUID());
		Set<ServerPlayer> viewers = VIEWERS.getOrDefault(wearer.getUUID(), Set.of());
		// Every player sees their own name in the third person view, with a title or without.
		Display.TextDisplay name = OWN_NAMES.computeIfAbsent(wearer.getUUID(), ignored -> create(wearer, NAME_HEIGHT));
		if (old != null) {
			// Without a title the game shows the name tag again; with a new one, both come again below.
			viewers.forEach(viewer -> viewer.connection.send(new ClientboundRemoveEntitiesPacket(old.getId(), name.getId())));
			wearer.connection.send(new ClientboundRemoveEntitiesPacket(old.getId()));
		}
		HIDDEN.put(wearer.getUUID(), state(wearer));
		Optional<Title> title = worn(wearer);
		if (title.isPresent() && PlayerTitles.config().aboveHead()) {
			Display.TextDisplay display = create(wearer, HEIGHT);
			DISPLAYS.put(wearer.getUUID(), display);
			viewers.forEach(viewer -> {
				spawnName(wearer, viewer);
				spawnFor(wearer, display, viewer);
			});
		}
		spawnOwn(wearer);
		wearer.level().getServer().getPlayerList().broadcastAll(
			new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(wearer)));
	}

	/** A viewer's client settings changed (maybe the language), or their animation setting: the titles they see, again. */
	public static void onOptionsChanged(ServerPlayer viewer) {
		if (viewer.connection == null) {
			return;
		}
		MinecraftServer server = viewer.level().getServer();
		for (Map.Entry<UUID, Display.TextDisplay> entry : DISPLAYS.entrySet()) {
			ServerPlayer wearer = server.getPlayerList().getPlayer(entry.getKey());
			if (wearer != null && (wearer == viewer || VIEWERS.getOrDefault(entry.getKey(), Set.of()).contains(viewer))) {
				viewer.connection.send(new ClientboundSetEntityDataPacket(entry.getValue().getId(), List.of(textFor(wearer, viewer))));
			}
		}
	}

	public static void onLeave(ServerPlayer player) {
		DISPLAYS.remove(player.getUUID());
		OWN_NAMES.remove(player.getUUID());
		SELF_SENT.remove(player.getUUID());
		SELF_LEVEL.remove(player.getUUID());
		HIDDEN.remove(player.getUUID());
		VIEWERS.remove(player.getUUID());
		VIEWERS.values().forEach(viewers -> viewers.remove(player));
	}

	public static void clear() {
		DISPLAYS.clear();
		OWN_NAMES.clear();
		SELF_SENT.clear();
		SELF_LEVEL.clear();
		HIDDEN.clear();
		VIEWERS.clear();
	}

	/** Every tick: animated titles move on, and titles hide while their wearer sneaks (or is invisible). */
	public static void onServerTick(MinecraftServer server) {
		animatePlayerList(server);
		int every = Math.max(1, PlayerTitles.config().animationTicks());
		boolean animate = server.getTickCount() % every == 0;
		for (ServerPlayer wearer : server.getPlayerList().getPlayers()) {
			UUID id = wearer.getUUID();
			if (!OWN_NAMES.containsKey(id)) {
				continue;
			}
			ServerPlayer sent = SELF_SENT.get(id);
			if (sent != wearer || sent.level() != SELF_LEVEL.get(id)) {
				spawnOwn(wearer);
			}
			int state = state(wearer);
			boolean changed = !Integer.valueOf(state).equals(HIDDEN.put(id, state));
			boolean hidden = state != 0;
			Display.TextDisplay display = DISPLAYS.get(id);
			if (changed) {
				wearer.connection.send(new ClientboundSetEntityDataPacket(OWN_NAMES.get(id).getId(), nameValues(wearer, wearer)));
				if (display != null) {
					for (ServerPlayer viewer : VIEWERS.getOrDefault(id, Set.of())) {
						viewer.connection.send(new ClientboundSetEntityDataPacket(OWN_NAMES.get(id).getId(), nameValues(wearer, viewer)));
					}
				}
			}
			if (display == null) {
				continue;
			}
			Map.Entry<UUID, Display.TextDisplay> entry = Map.entry(id, display);
			boolean moving = animate && worn(wearer).map(title -> title.styleOrPlain().animated()).orElse(false);
			if (changed || moving && !hidden) {
				// A new frame only for those near enough to read the name (vanilla's name tag distance); a change
				// between shown and hidden for everyone.
				// Players who switched animations off keep the still title.
				double reach = wearer.getAttributeValue(Attributes.NAME_TAG_DISTANCE);
				TitleData data = TitleData.get(server);
				for (ServerPlayer viewer : VIEWERS.getOrDefault(entry.getKey(), Set.of())) {
					if (!changed && (viewer.distanceToSqr(wearer) > reach * reach || !data.animations(viewer.getUUID()))) {
						continue;
					}
					viewer.connection.send(new ClientboundSetEntityDataPacket(entry.getValue().getId(), List.of(textFor(wearer, viewer))));
				}
				if (changed || data.animations(wearer.getUUID())) {
					wearer.connection.send(new ClientboundSetEntityDataPacket(entry.getValue().getId(), List.of(textFor(wearer, wearer))));
				}
			}
		}
	}

	/** A text display above an entity, at {@code height} over where its passengers sit; never added to the world. */
	public static Display.TextDisplay create(net.minecraft.world.entity.Entity wearer, float height) {
		Display.TextDisplay display = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, wearer.level());
		display.setPos(wearer.getX(), wearer.getY(), wearer.getZ());
		((DisplayAccessor) display).playerTitles$setBillboardConstraints(Display.BillboardConstraints.CENTER);
		((DisplayAccessor) display).playerTitles$setTransformation(new Transformation(new Vector3f(0.0F, height, 0.0F),
			new Quaternionf(), new Vector3f(1.0F), new Quaternionf()));
		return display;
	}

	/** A player's own name and title, for themselves (the game doesn't show a player their own name tag). */
	private static void spawnOwn(ServerPlayer wearer) {
		Display.TextDisplay display = DISPLAYS.get(wearer.getUUID());
		Display.TextDisplay name = OWN_NAMES.get(wearer.getUUID());
		if (name == null || wearer.connection == null) {
			return;
		}
		SELF_SENT.put(wearer.getUUID(), wearer);
		SELF_LEVEL.put(wearer.getUUID(), wearer.level());
		sendValues(wearer, name, wearer, nameValues(wearer, wearer));
		if (display != null) {
			send(wearer, display, wearer, textFor(wearer, wearer));
		}
		wearer.connection.send(new ClientboundSetPassengersPacket(wearer));
	}

	/** The name display for one viewer (with the title display, which keeps the game from showing the name tag). */
	private static void spawnName(ServerPlayer wearer, ServerPlayer viewer) {
		Display.TextDisplay name = OWN_NAMES.get(wearer.getUUID());
		if (name != null) {
			sendValues(wearer, name, viewer, nameValues(wearer, viewer));
		}
	}

	/**
	 * A name as the game shows name tags: see-through, on the default background; solid while sneaking; not while
	 * invisible or a spectator, nor where the wearer's team hides it from the viewer.
	 */
	public static List<SynchedEntityData.DataValue<?>> nameValues(net.minecraft.world.entity.Entity wearer, Component name, boolean shown,
			boolean sneaking) {
		byte flags = (byte) (Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND | (sneaking ? 0 : Display.TextDisplay.FLAG_SEE_THROUGH));
		return List.of(SynchedEntityData.DataValue.create(TextDisplayAccessor.playerTitles$textId(), shown ? name : Component.empty()),
			SynchedEntityData.DataValue.create(TextDisplayAccessor.playerTitles$flagsId(), flags));
	}

	private static List<SynchedEntityData.DataValue<?>> nameValues(ServerPlayer wearer, ServerPlayer viewer) {
		int state = state(wearer);
		return nameValues(wearer, wearer.getDisplayName(), state != 2 && teamShows(wearer, viewer), state == 1);
	}

	/** The game's rule for name tags of players in teams (LivingEntityRenderer.shouldShowName). */
	private static boolean teamShows(ServerPlayer wearer, ServerPlayer viewer) {
		if (wearer == viewer) {
			return true;
		}
		var team = wearer.getTeam();
		if (team == null) {
			return true;
		}
		var viewerTeam = viewer.getTeam();
		return switch (team.getNameTagVisibility()) {
			case ALWAYS -> true;
			case NEVER -> false;
			case HIDE_FOR_OTHER_TEAMS -> viewerTeam == null || team.isAlliedTo(viewerTeam);
			case HIDE_FOR_OWN_TEAM -> viewerTeam == null || !team.isAlliedTo(viewerTeam);
		};
	}

	/** Sends a display (made with create) with its data to a viewer; the passengers packet puts it on its entity. */
	public static void sendValues(net.minecraft.world.entity.Entity wearer, Display.TextDisplay display, ServerPlayer viewer,
			List<SynchedEntityData.DataValue<?>> extra) {
		viewer.connection.send(new ClientboundAddEntityPacket(display.getId(), display.getUUID(), wearer.getX(), wearer.getY(),
			wearer.getZ(), 0, 0, EntityTypes.TEXT_DISPLAY, 0, Vec3.ZERO, 0));
		List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();
		List<SynchedEntityData.DataValue<?>> nonDefault = display.getEntityData().getNonDefaultValues();
		if (nonDefault != null) {
			values.addAll(nonDefault);
		}
		values.addAll(extra);
		viewer.connection.send(new ClientboundSetEntityDataPacket(display.getId(), values));
	}

	private static void spawnFor(ServerPlayer wearer, Display.TextDisplay display, ServerPlayer viewer) {
		send(wearer, display, viewer, textFor(wearer, viewer));
		// Riding the player: the passengers packet now lists the display too (SetPassengersPacketMixin).
		viewer.connection.send(new ClientboundSetPassengersPacket(wearer));
	}

	/** Sends a display (made with create) and its text to a viewer; the passengers packet puts it on its entity. */
	public static void send(net.minecraft.world.entity.Entity wearer, Display.TextDisplay display, ServerPlayer viewer,
			SynchedEntityData.DataValue<Component> text) {
		viewer.connection.send(new ClientboundAddEntityPacket(display.getId(), display.getUUID(), wearer.getX(), wearer.getY(),
			wearer.getZ(), 0, 0, EntityTypes.TEXT_DISPLAY, 0, Vec3.ZERO, 0));
		List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();
		List<SynchedEntityData.DataValue<?>> nonDefault = display.getEntityData().getNonDefaultValues();
		if (nonDefault != null) {
			values.addAll(nonDefault);
		}
		values.add(text);
		viewer.connection.send(new ClientboundSetEntityDataPacket(display.getId(), values));
	}

	private static SynchedEntityData.DataValue<Component> textFor(ServerPlayer wearer, ServerPlayer viewer) {
		Component text = hidden(wearer) ? Component.empty()
			: worn(wearer).<Component>map(title -> TitleText.of(title, PlayerTitles.withForm(PlayerTitles.titleLanguage(viewer), wearer),
				TitleData.get(viewer.level().getServer()).animations(viewer.getUUID()) ? frame(wearer) : 0)).orElse(Component.empty());
		return SynchedEntityData.DataValue.create(TextDisplayAccessor.playerTitles$textId(), text);
	}

	private static boolean hidden(ServerPlayer wearer) {
		return state(wearer) != 0;
	}

	/** 0 shown, 1 sneaking (the title hides, the name shows solid, as name tags do), 2 hidden (invisible, spectator). */
	private static int state(ServerPlayer wearer) {
		return wearer.isInvisible() || wearer.isSpectator() ? 2 : wearer.isShiftKeyDown() ? 1 : 0;
	}

	private static long frame(ServerPlayer wearer) {
		return wearer.level().getServer().getTickCount() / Math.max(1, PlayerTitles.config().animationTicks());
	}

	private static Optional<Title> worn(ServerPlayer player) {
		String id = TitleData.get(player.level().getServer()).worn(player.getUUID());
		return id == null ? Optional.empty() : PlayerTitles.titles().get(id);
	}

	// --- Player list and chat ---

	/**
	 * The name in the player list: the title, then the name; null: vanilla's. Animated titles move there only with
	 * playerListAnimations, at its own pace (frames as above the head, fewer of them).
	 */
	public static @Nullable Component playerListName(ServerPlayer player, String language) {
		if (!PlayerTitles.config().playerList()) {
			return null;
		}
		long frame = PlayerTitles.config().playerListAnimations() ? frame(player) : 0;
		return worn(player).map(title -> withName(title, player, language, frame)).orElse(null);
	}

	/** With playerListAnimations: the list names of players with animated titles, again, to everyone who wants them. */
	private static void animatePlayerList(MinecraftServer server) {
		Config config = PlayerTitles.config();
		if (!config.playerList() || !config.playerListAnimations() || server.getTickCount() % config.playerListAnimationTicks() != 0) {
			return;
		}
		List<ServerPlayer> animated = server.getPlayerList().getPlayers().stream()
			.filter(player -> worn(player).map(title -> title.styleOrPlain().animated()).orElse(false)).toList();
		if (animated.isEmpty()) {
			return;
		}
		ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
			EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), animated);
		TitleData data = TitleData.get(server);
		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			if (data.animations(viewer.getUUID())) {
				viewer.connection.send(packet);
			}
		}
	}

	/**
	 * With each player's own language, a player list packet as one viewer gets it: the names of players with titles
	 * in the viewer's language (the game makes one packet for everyone, with the console's language).
	 */
	public static ClientboundPlayerInfoUpdatePacket forViewer(ClientboundPlayerInfoUpdatePacket packet, ServerPlayer viewer) {
		if (!PlayerTitles.config().perPlayerLanguage() || !packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)) {
			return packet;
		}
		String language = PlayerTitles.titleLanguage(viewer);
		if (language.equals(PlayerTitles.sharedLanguage())) {
			return packet;
		}
		MinecraftServer server = viewer.level().getServer();
		List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>();
		boolean changed = false;
		for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.profileId());
			Component name = player == null ? null : playerListName(player, language);
			if (name == null) {
				entries.add(entry);
			} else {
				changed = true;
				entries.add(new ClientboundPlayerInfoUpdatePacket.Entry(entry.profileId(), entry.profile(), entry.listed(), entry.latency(),
					entry.gameMode(), name, entry.showHat(), entry.listOrder(), entry.chatSession()));
			}
		}
		if (!changed) {
			return packet;
		}
		ClientboundPlayerInfoUpdatePacket copy = new ClientboundPlayerInfoUpdatePacket(packet.actions(), List.of());
		((PlayerInfoPacketAccessor) copy).playerTitles$setEntries(entries);
		return copy;
	}

	/**
	 * The title shown before a chat message's name (in the server's language), with its description on hover; null
	 * without one.
	 */
	public static @Nullable Component chatTitle(ServerPlayer player, String language) {
		if (!PlayerTitles.config().chat()) {
			return null;
		}
		String formed = PlayerTitles.withForm(language, player);
		return worn(player).<Component>map(title -> {
			String description = title.description(language);
			MutableComponent text = TitleText.of(title, formed, 0);
			if (!description.isEmpty()) {
				Component hover = TitleText.of(title, formed, 0).append(Component.literal("\n" + String.join("\n", TextWidth.wrap(description, TextWidth.TOOLTIP))).withStyle(ChatFormatting.GRAY));
				text.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));
			}
			return text;
		}).orElse(null);
	}

	/**
	 * The chat type a player's message is sent with: the configured format (chatFormat), the title before it (riding in
	 * the "target" parameter, which plain chat doesn't use). It goes along inline in the packet (not a registry entry);
	 * clients show a format that isn't a translation key of theirs as it is. The message's signature covers neither
	 * name nor format, so signed chat stays intact.
	 */
	public static ChatType.Bound chatType(ServerPlayer sender, String language) {
		String format = PlayerTitles.config().chatFormat();
		Component title = chatTitle(sender, language);
		if (format.equals(VANILLA_CHAT_FORMAT) && title == null) {
			return ChatType.bind(ChatType.CHAT, sender);
		}
		ChatType vanilla = sender.level().registryAccess().lookupOrThrow(Registries.CHAT_TYPE).getOrThrow(ChatType.CHAT).value();
		ChatTypeDecoration decoration = title == null ? ChatTypeDecoration.withSender(format)
			: new ChatTypeDecoration("%s " + format, List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.SENDER,
				ChatTypeDecoration.Parameter.CONTENT), Style.EMPTY);
		return new ChatType.Bound(Holder.direct(new ChatType(decoration, vanilla.narration())), sender.getDisplayName(), Optional.ofNullable(title));
	}

	/** The sender of the chat message being sent right now (ServerGamePacketListenerImplMixin), for per-player languages. */
	private static final ThreadLocal<ServerPlayer> CHAT_SENDER = new ThreadLocal<>();

	public static void chatSending(@Nullable ServerPlayer sender) {
		if (sender == null) {
			CHAT_SENDER.remove();
		} else {
			CHAT_SENDER.set(sender);
		}
	}

	/** The chat type of the message being sent, for one receiver: with their language (ServerPlayerMixin). */
	public static ChatType.Bound chatTypeFor(ChatType.Bound bound, ServerPlayer receiver) {
		ServerPlayer sender = CHAT_SENDER.get();
		if (sender == null || !PlayerTitles.config().perPlayerLanguage() || PlayerTitles.titleLanguage(receiver).equals(PlayerTitles.sharedLanguage())) {
			return bound;
		}
		return chatType(sender, PlayerTitles.titleLanguage(receiver));
	}

	private static final String VANILLA_CHAT_FORMAT = "<%s> %s";

	private static Component withName(Title title, ServerPlayer player, String language, long frame) {
		MutableComponent name = TitleText.of(title, PlayerTitles.withForm(language, player), frame);
		return name.append(Component.literal(" ")).append(player.getDisplayName());
	}
}
