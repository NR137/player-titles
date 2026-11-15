package de.nr137.playertitles.merchant;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.npc.villager.Villager;
import de.nr137.lib.lang.Translations;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.display.TitleDisplays;
import de.nr137.playertitles.mixin.TextDisplayAccessor;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleStyle;
import de.nr137.playertitles.title.TitleText;

/**
 * The title above a Title Merchant ("Title Merchant", animated, with name tags at both ends), shown like a player's
 * title: a text display riding the villager, only in the packets, in each viewer's language. The villager's own name
 * stays its vanilla name, shown as vanilla shows names (when looked at, close by).
 */
public final class MerchantBadges {
	private static final TitleStyle STYLE = new TitleStyle(null, List.of("#B6FF8A", "#1FAA59", "#7DFFE0"), false, false, "wave", null,
		List.of("item/name_tag"), null, null, false);
	/** Every villager's viewers (a villager may become a merchant while seen). */
	private static final Map<UUID, Set<ServerPlayer>> VIEWERS = new HashMap<>();
	private static final Map<UUID, Display.TextDisplay> DISPLAYS = new HashMap<>();
	/**
	 * Each merchant's name, where the name tag is: the game shows no name on an entity that carries a passenger, like
	 * this title. Shown always (vanilla shows a mob's name only when looked at, close by), and not through walls.
	 */
	private static final Map<UUID, Display.TextDisplay> NAMES = new HashMap<>();
	private static final Map<UUID, Component> SHOWN_NAMES = new HashMap<>();
	private static final Map<UUID, Villager> MERCHANTS = new HashMap<>();
	private static Title badge;

	private MerchantBadges() {
	}

	public static void init() {
		Map<String, String> text = new HashMap<>();
		for (String language : Translations.LANGUAGES) {
			text.put(language, PlayerTitles.translations().text(language, "merchant.player_titles.badge"));
		}
		badge = new Title("merchant_badge", text, null, STYLE, "legendary", List.of(), null);
	}

	/** The game starts sending a villager to a player (ServerEntityMixin). */
	public static void onSeen(Villager villager, ServerPlayer viewer) {
		VIEWERS.computeIfAbsent(villager.getUUID(), ignored -> new HashSet<>()).add(viewer);
		if (TitleMerchants.isMerchant(villager)) {
			spawnFor(villager, display(villager), viewer);
		}
	}

	public static void onUnseen(Villager villager, ServerPlayer viewer) {
		Set<ServerPlayer> viewers = VIEWERS.get(villager.getUUID());
		if (viewers != null) {
			viewers.remove(viewer);
			if (viewers.isEmpty()) {
				VIEWERS.remove(villager.getUUID());
				MERCHANTS.remove(villager.getUUID());
			}
		}
		Display.TextDisplay display = DISPLAYS.get(villager.getUUID());
		if (display != null) {
			viewer.connection.send(new ClientboundRemoveEntitiesPacket(display.getId(), NAMES.get(villager.getUUID()).getId()));
		}
	}

	/** A villager became a merchant: everyone who sees it gets its title, and sees what it holds now. */
	public static void refresh(Villager villager) {
		Display.TextDisplay display = display(villager);
		var equipment = new ClientboundSetEquipmentPacket(villager.getId(), List.of(Pair.of(EquipmentSlot.MAINHAND, villager.getMainHandItem())));
		VIEWERS.getOrDefault(villager.getUUID(), Set.of()).forEach(viewer -> {
			spawnFor(villager, display, viewer);
			viewer.connection.send(equipment);
		});
	}

	public static void remove(Villager villager) {
		Display.TextDisplay display = DISPLAYS.remove(villager.getUUID());
		Display.TextDisplay name = NAMES.remove(villager.getUUID());
		MERCHANTS.remove(villager.getUUID());
		SHOWN_NAMES.remove(villager.getUUID());
		if (display != null) {
			VIEWERS.getOrDefault(villager.getUUID(), Set.of())
				.forEach(viewer -> viewer.connection.send(new ClientboundRemoveEntitiesPacket(display.getId(), name.getId())));
		}
	}

	/**
	 * What a Title Merchant holds, as players see it: today's currency in its hand, its name tag in the other (the game
	 * doesn't draw a villager's offhand). Only in the packets: saved, it holds the name tag in its hand.
	 */
	public static ClientboundSetEquipmentPacket equipmentFor(ClientboundSetEquipmentPacket packet, ServerPlayer viewer) {
		if (!(viewer.level().getEntity(packet.getEntity()) instanceof Villager villager) || !TitleMerchants.isMerchant(villager)) {
			return packet;
		}
		List<Pair<EquipmentSlot, ItemStack>> slots = new ArrayList<>();
		for (Pair<EquipmentSlot, ItemStack> slot : packet.getSlots()) {
			if (slot.getFirst() != EquipmentSlot.MAINHAND && slot.getFirst() != EquipmentSlot.OFFHAND) {
				slots.add(slot);
			}
		}
		slots.add(Pair.of(EquipmentSlot.MAINHAND, new ItemStack(TitleMerchants.currency(TitleMerchants.today()))));
		slots.add(Pair.of(EquipmentSlot.OFFHAND, new ItemStack(Items.NAME_TAG)));
		return new ClientboundSetEquipmentPacket(packet.getEntity(), slots);
	}

	/** A new day: merchants hold the new currency (sent again to everyone who sees them). */
	private static void onNewDay() {
		for (Map.Entry<UUID, Villager> entry : MERCHANTS.entrySet()) {
			Villager villager = entry.getValue();
			var packet = new ClientboundSetEquipmentPacket(villager.getId(), List.of(Pair.of(EquipmentSlot.MAINHAND, villager.getMainHandItem())));
			VIEWERS.getOrDefault(entry.getKey(), Set.of()).forEach(viewer -> viewer.connection.send(packet));
		}
	}

	private static long day = Long.MIN_VALUE;

	/** The display riding a villager, for the passengers packet (SetPassengersPacketMixin); -1 if none. */
	public static int displayId(Villager villager) {
		Display.TextDisplay display = DISPLAYS.get(villager.getUUID());
		return display == null ? -1 : display.getId();
	}

	public static int nameId(Villager villager) {
		Display.TextDisplay name = NAMES.get(villager.getUUID());
		return name == null ? -1 : name.getId();
	}

	/** A player left: they see no villager any more. */
	public static void forget(ServerPlayer player) {
		VIEWERS.values().forEach(viewers -> viewers.remove(player));
	}

	public static void clear() {
		VIEWERS.clear();
		DISPLAYS.clear();
		NAMES.clear();
		SHOWN_NAMES.clear();
		MERCHANTS.clear();
	}

	/** The animation moves on, for viewers near enough to read it who have animated titles on. */
	public static void onServerTick(MinecraftServer server) {
		if (server.getTickCount() % 20 == 0) {
			long today = TitleMerchants.today();
			if (day != Long.MIN_VALUE && today != day) {
				onNewDay();
			}
			day = today;
			// A merchant renamed (a name tag) or turned invisible: its name again.
			for (Map.Entry<UUID, Villager> entry : MERCHANTS.entrySet()) {
				Component name = shownName(entry.getValue());
				if (!name.equals(SHOWN_NAMES.put(entry.getKey(), name)) && NAMES.containsKey(entry.getKey())) {
					var packet = new ClientboundSetEntityDataPacket(NAMES.get(entry.getKey()).getId(), nameValues(entry.getValue()));
					VIEWERS.getOrDefault(entry.getKey(), Set.of()).forEach(viewer -> viewer.connection.send(packet));
				}
			}
		}
		int every = Math.max(1, PlayerTitles.config().animationTicks());
		if (server.getTickCount() % every != 0 || MERCHANTS.isEmpty()) {
			return;
		}
		TitleData data = TitleData.get(server);
		for (Map.Entry<UUID, Villager> entry : MERCHANTS.entrySet()) {
			Display.TextDisplay display = DISPLAYS.get(entry.getKey());
			if (display == null || entry.getValue().isRemoved()) {
				continue;
			}
			for (ServerPlayer viewer : VIEWERS.getOrDefault(entry.getKey(), Set.of())) {
				if (viewer.distanceToSqr(entry.getValue()) <= 64 * 64 && data.animations(viewer.getUUID())) {
					viewer.connection.send(new ClientboundSetEntityDataPacket(display.getId(), List.of(text(entry.getValue(), viewer))));
				}
			}
		}
	}

	private static Display.TextDisplay display(Villager villager) {
		MERCHANTS.put(villager.getUUID(), villager);
		NAMES.computeIfAbsent(villager.getUUID(), ignored -> TitleDisplays.create(villager, TitleDisplays.NAME_HEIGHT));
		SHOWN_NAMES.put(villager.getUUID(), shownName(villager));
		return DISPLAYS.computeIfAbsent(villager.getUUID(), ignored -> TitleDisplays.create(villager, TitleDisplays.HEIGHT));
	}

	private static Component shownName(Villager villager) {
		return villager.isInvisible() ? Component.empty() : villager.getDisplayName();
	}

	private static List<SynchedEntityData.DataValue<?>> nameValues(Villager villager) {
		// Solid, like its title: neither shows through walls.
		return TitleDisplays.nameValues(villager, villager.getDisplayName(), !villager.isInvisible() && villager.hasCustomName(), true);
	}

	private static void spawnFor(Villager villager, Display.TextDisplay display, ServerPlayer viewer) {
		TitleDisplays.sendValues(villager, NAMES.get(villager.getUUID()), viewer, nameValues(villager));
		TitleDisplays.send(villager, display, viewer, text(villager, viewer));
		viewer.connection.send(new ClientboundSetPassengersPacket(villager));
	}

	private static SynchedEntityData.DataValue<Component> text(Villager villager, ServerPlayer viewer) {
		long frame = TitleData.get(viewer.level().getServer()).animations(viewer.getUUID())
			? viewer.level().getServer().getTickCount() / Math.max(1, PlayerTitles.config().animationTicks()) : 0;
		Component text = villager.isInvisible() ? Component.empty() : TitleText.of(badge, PlayerTitles.titleLanguage(viewer), frame);
		return SynchedEntityData.DataValue.create(TextDisplayAccessor.playerTitles$textId(), text);
	}
}
