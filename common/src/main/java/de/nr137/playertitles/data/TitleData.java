package de.nr137.playertitles.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import de.nr137.playertitles.PlayerTitles;

/**
 * What every player owns and wears, saved as {@code <world>/data/player_titles/players.dat} (vanilla ignores it). A
 * title stays owned even if its definition is removed (it just isn't shown until it comes back).
 */
public final class TitleData extends SavedData {
	/** One owned title: where it came from ("admin", "advancement:...", ...) and when (epoch milliseconds). */
	public record Owned(String source, long time) {
		static final Codec<Owned> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("source").forGetter(Owned::source),
			Codec.LONG.fieldOf("time").forGetter(Owned::time)
		).apply(instance, Owned::new));
	}

	/**
	 * One player's titles, the one they wear, and whether they see animated titles move (off: fewer packets for them,
	 * the titles stand still).
	 */
	public record PlayerRecord(Map<String, Owned> owned, Optional<String> worn, boolean animations, Optional<String> form) {
		static final Codec<PlayerRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, Owned.CODEC).optionalFieldOf("owned", Map.of()).forGetter(PlayerRecord::owned),
			Codec.STRING.optionalFieldOf("worn").forGetter(PlayerRecord::worn),
			Codec.BOOL.optionalFieldOf("animations", true).forGetter(PlayerRecord::animations),
			Codec.STRING.optionalFieldOf("form").forGetter(PlayerRecord::form)
		).apply(instance, PlayerRecord::new));
	}

	/** A player's dealings with Title Merchants: trades so far, and the day and titles bought that day. */
	public record Trades(int count, long day, List<String> bought) {
		static final Codec<Trades> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("count", 0).forGetter(Trades::count),
			Codec.LONG.optionalFieldOf("day", 0L).forGetter(Trades::day),
			Codec.STRING.listOf().optionalFieldOf("bought", List.of()).forGetter(Trades::bought)
		).apply(instance, Trades::new));

		public static final Trades NONE = new Trades(0, 0, List.of());

		/** The titles bought on {@code today} (none if the last purchase was another day). */
		public List<String> boughtOn(long today) {
			return day == today ? bought : List.of();
		}
	}

	private static final Codec<TitleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.unboundedMap(Codec.STRING, PlayerRecord.CODEC).optionalFieldOf("players", Map.of()).forGetter(data -> data.players),
		Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("first", Map.of()).forGetter(data -> data.firsts),
		Codec.STRING.listOf().optionalFieldOf("merchants", List.of()).forGetter(data -> List.copyOf(data.merchants)),
		Codec.unboundedMap(Codec.STRING, Trades.CODEC).optionalFieldOf("trades", Map.of()).forGetter(data -> data.trades)
	).apply(instance, TitleData::new));

	public static final SavedDataType<TitleData> TYPE = new SavedDataType<>(PlayerTitles.id("players"), TitleData::new, CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<String, PlayerRecord> players;
	/** Advancement id -> the player (UUID) who completed it first on the server: holds its "first" title for good. */
	private final Map<String, String> firsts;

	/** The villagers (UUIDs) that are Title Merchants; on the villager itself nothing is saved. */
	private final java.util.Set<String> merchants;
	private final Map<String, Trades> trades;

	public TitleData() {
		this(Map.of(), Map.of(), List.of(), Map.of());
	}

	private TitleData(Map<String, PlayerRecord> players, Map<String, String> firsts, List<String> merchants, Map<String, Trades> trades) {
		this.players = new HashMap<>(players);
		this.firsts = new HashMap<>(firsts);
		this.merchants = new java.util.HashSet<>(merchants);
		this.trades = new HashMap<>(trades);
	}

	public boolean isMerchant(UUID villager) {
		return merchants.contains(villager.toString());
	}

	public void setMerchant(UUID villager, boolean merchant) {
		if (merchant ? merchants.add(villager.toString()) : merchants.remove(villager.toString())) {
			setDirty();
		}
	}

	public Trades trades(UUID player) {
		return trades.getOrDefault(player.toString(), Trades.NONE);
	}

	/** A title bought on {@code today}: one more trade, and today's list. */
	public void bought(UUID player, String title, long today) {
		Trades before = trades(player);
		List<String> bought = new java.util.ArrayList<>(before.boughtOn(today));
		bought.add(title);
		trades.put(player.toString(), new Trades(before.count() + 1, today, bought));
		setDirty();
	}

	public Optional<UUID> firstHolder(String advancement) {
		return Optional.ofNullable(firsts.get(advancement)).map(UUID::fromString);
	}

	/** Forgets who was first (the title stays with its holder). */
	public void releaseFirst(String advancement) {
		if (firsts.remove(advancement) != null) {
			setDirty();
		}
	}

	/** Who was first of which advancement: advancement id -> player. */
	public Map<String, UUID> firsts() {
		Map<String, UUID> result = new HashMap<>();
		firsts.forEach((advancement, player) -> result.put(advancement, UUID.fromString(player)));
		return result;
	}

	/** Makes {@code player} the first of an advancement, if nobody is yet; true if they are now. */
	public boolean claimFirst(String advancement, UUID player) {
		if (firsts.putIfAbsent(advancement, player.toString()) != null) {
			return false;
		}
		setDirty();
		return true;
	}

	public static TitleData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public PlayerRecord of(UUID player) {
		return players.getOrDefault(player.toString(), new PlayerRecord(Map.of(), Optional.empty(), true, Optional.empty()));
	}

	public boolean owns(UUID player, String title) {
		return of(player).owned().containsKey(title);
	}

	public @Nullable String worn(UUID player) {
		return of(player).worn().orElse(null);
	}

	/** Gives a title; returns false if the player had it already. */
	public boolean give(UUID player, String title, String source) {
		PlayerRecord record = of(player);
		if (record.owned().containsKey(title)) {
			return false;
		}
		Map<String, Owned> owned = new LinkedHashMap<>(record.owned());
		owned.put(title, new Owned(source, System.currentTimeMillis()));
		put(player, new PlayerRecord(owned, record.worn(), record.animations(), record.form()));
		return true;
	}

	/** Takes a title away (and takes it off if worn); returns false if the player didn't have it. */
	public boolean take(UUID player, String title) {
		PlayerRecord record = of(player);
		if (!record.owned().containsKey(title)) {
			return false;
		}
		Map<String, Owned> owned = new LinkedHashMap<>(record.owned());
		owned.remove(title);
		put(player, new PlayerRecord(owned, record.worn().filter(worn -> !worn.equals(title)), record.animations(), record.form()));
		return true;
	}

	/** Wears an owned title, or none (null). */
	public void wear(UUID player, @Nullable String title) {
		PlayerRecord record = of(player);
		put(player, new PlayerRecord(record.owned(), Optional.ofNullable(title).filter(record.owned()::containsKey), record.animations(), record.form()));
	}

	public boolean animations(UUID player) {
		return of(player).animations();
	}

	public void setAnimations(UUID player, boolean on) {
		PlayerRecord record = of(player);
		put(player, new PlayerRecord(record.owned(), record.worn(), on, record.form()));
	}

	/** The form the player chose for their titles ("male", "female", "neutral"), if any. */
	public Optional<String> form(UUID player) {
		return of(player).form();
	}

	public void setForm(UUID player, String form) {
		PlayerRecord record = of(player);
		put(player, new PlayerRecord(record.owned(), record.worn(), record.animations(), Optional.of(form)));
	}

	private void put(UUID player, PlayerRecord record) {
		if (record.owned().isEmpty() && record.worn().isEmpty() && record.animations() && record.form().isEmpty()) {
			players.remove(player.toString());
		} else {
			players.put(player.toString(), record);
		}
		setDirty();
	}
}
