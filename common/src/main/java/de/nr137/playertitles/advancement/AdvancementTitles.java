package de.nr137.playertitles.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.display.TitleDisplays;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleLibrary;
import de.nr137.playertitles.title.TitleText;

/**
 * Advancement titles: completing an advancement gives its title, losing it (a new game version changed it, an admin
 * revoked it) takes the title away. The first player on the server to complete one also gets its "first" title, which
 * stays theirs for good and exists only once. Holders are backfilled at every start from the dates in the players'
 * advancement files (for advancements nobody holds yet), so a server that adds the mod later still gets them right.
 */
public final class AdvancementTitles {
	private AdvancementTitles() {
	}

	/** An advancement's progress changed (PlayerAdvancementsMixin, after award and revoke). */
	public static void onProgress(ServerPlayer player, AdvancementHolder advancement, boolean done) {
		String id = advancement.id().toString();
		TitleLibrary titles = PlayerTitles.titles();
		Title title = titles.advancementTitles().get(id);
		TitleData data = TitleData.get(player.level().getServer());
		boolean online = player.connection != null;
		boolean changed = false;
		if (title != null) {
			if (done && data.give(player.getUUID(), title.id(), TitleLibrary.ADVANCEMENT + id)) {
				changed = true;
				if (online) {
					player.sendSystemMessage(PlayerTitles.translations().component(PlayerTitles.uiLanguage(player), "message.player_titles.got")
						.append(styled(title, player)));
				}
			} else if (!done && data.take(player.getUUID(), title.id())) {
				changed = true;
			}
		}
		Title first = titles.firstAdvancementTitles().get(id);
		if (done && first != null && data.claimFirst(id, player.getUUID())) {
			data.give(player.getUUID(), first.id(), TitleLibrary.FIRST_ADVANCEMENT + id);
			changed = true;
			if (online) {
				announceFirst(player, first);
			}
		}
		changed |= checkAll(player, online);
		if (changed && online) {
			TitleDisplays.refresh(player);
		}
	}

	/** The key of the "first on the server" holder of all advancements (TitleData's firsts). */
	public static final String ALL = "player_titles:all_advancements";
	public static final String ALL_SOURCE = "all_advancements";
	public static final String FIRST_ALL_SOURCE = "first_all_advancements";

	/**
	 * Every advancement with a title (of this game version) done: the title for all of them ("all_advancements"), and
	 * for the first player its "first" one; losing one takes the first again, the "first" one stays. True if changed.
	 */
	private static boolean checkAll(ServerPlayer player, boolean online) {
		Title all = bySource(ALL_SOURCE);
		Title first = bySource(FIRST_ALL_SOURCE);
		if (all == null && first == null) {
			return false;
		}
		MinecraftServer server = player.level().getServer();
		boolean done = true;
		for (String id : PlayerTitles.titles().advancementTitles().keySet()) {
			AdvancementHolder advancement = server.getAdvancements().get(Identifier.parse(id));
			if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
				done = false;
				break;
			}
		}
		TitleData data = TitleData.get(server);
		boolean changed = false;
		if (all != null) {
			changed = done ? data.give(player.getUUID(), all.id(), ALL_SOURCE) : data.take(player.getUUID(), all.id());
			if (changed && done && online) {
				player.sendSystemMessage(PlayerTitles.translations().component(PlayerTitles.uiLanguage(player), "message.player_titles.got")
					.append(styled(all, player)));
			}
		}
		if (done && first != null && data.claimFirst(ALL, player.getUUID())) {
			data.give(player.getUUID(), first.id(), FIRST_ALL_SOURCE);
			changed = true;
			if (online) {
				announceFirst(player, first);
			}
		}
		return changed;
	}

	private static @Nullable Title bySource(String source) {
		return PlayerTitles.titles().all().stream().filter(title -> title.sourcesOrNone().contains(source)).findFirst().orElse(null);
	}

	/**
	 * When a player joins: their advancement titles as their advancements are now (the game checks the progress again
	 * against the advancements of this version while loading it). Quietly, with one message for what's new.
	 */
	public static void onJoin(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		TitleData data = TitleData.get(server);
		int gained = 0;
		for (Map.Entry<String, Title> entry : PlayerTitles.titles().advancementTitles().entrySet()) {
			AdvancementHolder advancement = server.getAdvancements().get(Identifier.parse(entry.getKey()));
			if (advancement == null) {
				continue; // not in this game (version): the title stays as it is
			}
			boolean done = player.getAdvancements().getOrStartProgress(advancement).isDone();
			if (done && data.give(player.getUUID(), entry.getValue().id(), TitleLibrary.ADVANCEMENT + entry.getKey())) {
				gained++;
			} else if (!done) {
				data.take(player.getUUID(), entry.getValue().id());
			}
			Title first = PlayerTitles.titles().firstAdvancementTitles().get(entry.getKey());
			if (done && first != null && data.claimFirst(entry.getKey(), player.getUUID())) {
				data.give(player.getUUID(), first.id(), TitleLibrary.FIRST_ADVANCEMENT + entry.getKey());
				gained++;
			}
		}
		if (checkAll(player, false)) {
			gained++;
		}
		if (gained > 0) {
			player.sendSystemMessage(PlayerTitles.translations().component(PlayerTitles.uiLanguage(player), "message.player_titles.got_many", gained));
		}
	}

	/**
	 * At every start: for each advancement with a "first" title nobody holds, the player who completed it first, from
	 * the advancement files of all players who ever played (online or not). They get the title.
	 */
	public static void backfill(MinecraftServer server) {
		TitleData data = TitleData.get(server);
		Map<String, Title> open = new HashMap<>();
		PlayerTitles.titles().firstAdvancementTitles().forEach((id, title) -> {
			if (data.firstHolder(id).isEmpty()) {
				open.put(id, title);
			}
		});
		Title firstAll = bySource(FIRST_ALL_SOURCE);
		boolean allOpen = firstAll != null && data.firstHolder(ALL).isEmpty();
		if (open.isEmpty() && !allOpen) {
			return;
		}
		UUID allHolder = null;
		Instant allTime = null;
		Map<String, UUID> holders = new HashMap<>();
		Map<String, Instant> times = new HashMap<>();
		Path folder = server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
				UUID player = uuid(file);
				if (player == null) {
					continue;
				}
				JsonObject json;
				try {
					json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
				} catch (IOException | RuntimeException e) {
					PlayerTitles.LOGGER.warn("Can't read the advancements in {}: {}", file, e.toString());
					continue;
				}
				for (String id : open.keySet()) {
					Instant done = completed(server, id, json.get(id));
					if (done != null && (times.get(id) == null || done.isBefore(times.get(id)))) {
						times.put(id, done);
						holders.put(id, player);
					}
				}
				if (allOpen) {
					// All done when the last one was: the earliest such player is the first.
					Instant last = Instant.EPOCH;
					for (String id : PlayerTitles.titles().advancementTitles().keySet()) {
						if (server.getAdvancements().get(Identifier.parse(id)) == null) {
							continue;
						}
						Instant done = completed(server, id, json.get(id));
						if (done == null) {
							last = null;
							break;
						}
						if (done.isAfter(last)) {
							last = done;
						}
					}
					if (last != null && (allTime == null || last.isBefore(allTime))) {
						allTime = last;
						allHolder = player;
					}
				}
			}
		} catch (IOException e) {
			PlayerTitles.LOGGER.error("Can't read the advancement files in {}: {}", folder, e.toString());
			return;
		}
		if (allHolder != null && data.claimFirst(ALL, allHolder)) {
			data.give(allHolder, firstAll.id(), FIRST_ALL_SOURCE);
		}
		holders.forEach((id, player) -> {
			if (data.claimFirst(id, player)) {
				data.give(player, open.get(id).id(), TitleLibrary.FIRST_ADVANCEMENT + id);
			}
		});
		if (!holders.isEmpty()) {
			PlayerTitles.LOGGER.info("Gave {} \"first on the server\" titles from the players' advancements", holders.size());
		}
	}

	/** When an advancement was completed in a player's file (with this version's requirements), or null if it isn't. */
	private static @Nullable Instant completed(MinecraftServer server, String id, @Nullable JsonElement json) {
		AdvancementHolder advancement = json == null ? null : server.getAdvancements().get(Identifier.parse(id));
		if (advancement == null) {
			return null;
		}
		AdvancementProgress progress = AdvancementProgress.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
		if (progress == null) {
			return null;
		}
		AdvancementRequirements requirements = advancement.value().requirements();
		progress.update(requirements);
		if (!progress.isDone()) {
			return null;
		}
		// Done when the last of its requirements was met; a requirement is met by the first of its criteria.
		Instant done = Instant.EPOCH;
		for (List<String> requirement : requirements.requirements()) {
			Instant met = null;
			for (String criterion : requirement) {
				CriterionProgress criterionProgress = progress.getCriterion(criterion);
				Instant obtained = criterionProgress == null ? null : criterionProgress.getObtained();
				if (obtained != null && (met == null || obtained.isBefore(met))) {
					met = obtained;
				}
			}
			if (met != null && met.isAfter(done)) {
				done = met;
			}
		}
		return done;
	}

	private static @Nullable UUID uuid(Path file) {
		String name = file.getFileName().toString();
		try {
			return UUID.fromString(name.substring(0, name.length() - ".json".length()));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** Everyone hears who was first, each in their language. */
	private static void announceFirst(ServerPlayer player, Title first) {
		for (ServerPlayer receiver : player.level().getServer().getPlayerList().getPlayers()) {
			Component name = TitleText.of(first, PlayerTitles.withForm(PlayerTitles.titleLanguage(receiver), player), 0);
			receiver.sendSystemMessage(PlayerTitles.translations().component(PlayerTitles.uiLanguage(receiver), "message.player_titles.first",
				player.getName().getString()).append(name));
		}
	}

	private static Component styled(Title title, ServerPlayer player) {
		return TitleText.of(title, PlayerTitles.withForm(PlayerTitles.titleLanguage(player), player), 0);
	}
}
