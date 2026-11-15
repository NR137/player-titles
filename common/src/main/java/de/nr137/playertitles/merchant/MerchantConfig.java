package de.nr137.playertitles.merchant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Title Merchant's options ("merchant" in config.json).
 *
 * @param villagers                   which villagers become merchants by picking up a name tag: "nitwits",
 *                                    "unemployed" or "both" (unemployed merchants never take a job)
 * @param breedNitwitChance           bred villagers become nitwits with this chance (0 to 1; vanilla: 0, they never
 *                                    do); 1/12 is the share of nitwits in vanilla's villages
 * @param nitwitKillsAffectReputation hurting or killing a nitwit gives the villagers' usual bad gossip (vanilla); off:
 *                                    it doesn't, so unwanted nitwits can be removed (Title Merchants always do)
 * @param offers                      how many titles a player is offered a day at first
 * @param maxOffers                   how many at the highest level
 * @param levels                      titles bought for each further offer and merchant level (5: the 4th offer and
 *                                    Apprentice after 5, ...; the trading screen shows up to Master)
 * @param currency                    the items titles cost, one a day in turn, with a factor on the prices: items
 *                                    players get anyway and can't do much with; the easier to get many of, the more
 *                                    it takes
 * @param prices                      the price per rarity (times the currency's factor)
 * @param slotChances                 for each offer, in order, how likely each rarity is (weights; the last one
 *                                    for any further offers); a rarity the player has no titles of left is skipped
 * @param reputationPrices            prices follow the merchant's gossip about the player, as vanilla villagers'
 *                                    do: dearer after hitting it or killing villagers near it, cheaper after trading
 *                                    or curing; and the Hero of the Village discount (from half to three times)
 * @param killTitleChance             the chance (0 to 1) that a player who kills a Title Merchant gets the secret
 *                                    titles with the source "merchant_kill", 1 % more per Looting level (as the
 *                                    game's rare drops); a merchant drops nothing it holds
 * @param screen                      "trading": the game's trading screen (with the merchant's level for the player),
 *                                    "chest": a chest screen with the offers
 */
public record MerchantConfig(String villagers, double breedNitwitChance, boolean nitwitKillsAffectReputation, int offers,
		int maxOffers, List<Integer> levels, Map<String, Double> currency, Map<String, Integer> prices,
		List<Map<String, Integer>> slotChances, boolean reputationPrices, double killTitleChance,
		String screen) {
	public static final List<String> VILLAGERS = List.of("nitwits", "unemployed", "both");
	public static final List<String> RARITIES = List.of("common", "rare", "epic", "legendary");

	public static MerchantConfig defaults() {
		Map<String, Double> currency = new LinkedHashMap<>();
		currency.put("minecraft:rotten_flesh", 1.5);
		currency.put("minecraft:poisonous_potato", 0.5);
		currency.put("minecraft:spider_eye", 1.0);
		currency.put("minecraft:pufferfish", 0.5);
		currency.put("minecraft:glow_lichen", 1.0);
		currency.put("minecraft:leaf_litter", 2.0);
		Map<String, Integer> prices = new LinkedHashMap<>();
		prices.put("common", 8);
		prices.put("rare", 16);
		prices.put("epic", 32);
		prices.put("legendary", 48);
		List<Map<String, Integer>> slots = List.of(
			chances(80, 20, 0, 0),
			chances(60, 35, 5, 0),
			chances(40, 40, 17, 3),
			chances(0, 50, 40, 10),
			chances(0, 30, 50, 20),
			chances(0, 0, 60, 40),
			chances(0, 0, 40, 60));
		return new MerchantConfig("nitwits", 1.0 / 12, false, 3, 7, List.of(5, 15, 30, 50), currency, prices, slots, true, 0.02, "trading");
	}

	private static Map<String, Integer> chances(int common, int rare, int epic, int legendary) {
		Map<String, Integer> chances = new LinkedHashMap<>();
		chances.put("common", common);
		chances.put("rare", rare);
		chances.put("epic", epic);
		chances.put("legendary", legendary);
		return chances;
	}

	public boolean nitwitsCan() {
		return !"unemployed".equals(villagers);
	}

	public boolean unemployedCan() {
		return "unemployed".equals(villagers) || "both".equals(villagers);
	}

	/** A player's offers a day after {@code trades} titles bought. */
	public int offersAfter(int trades) {
		int count = offers;
		for (int needed : levels == null ? List.<Integer>of() : levels) {
			if (trades >= needed) {
				count++;
			}
		}
		return Math.min(count, maxOffers);
	}

	/** The price of a rarity in a currency (before reputation). */
	public int price(String rarity, String currencyItem) {
		int base = Math.max(1, prices.getOrDefault(rarity == null ? "common" : rarity, prices.getOrDefault("common", 8)));
		double factor = currency == null ? 1.0 : currency.getOrDefault(currencyItem, 1.0);
		return Math.max(1, (int) Math.round(base * factor));
	}

	/** How likely each rarity is for offer {@code slot} (0-based). */
	public Map<String, Integer> chancesFor(int slot) {
		if (slotChances == null || slotChances.isEmpty()) {
			return chances(60, 25, 12, 3);
		}
		return slotChances.get(Math.min(slot, slotChances.size() - 1));
	}

	public MerchantConfig withVillagers(String value) {
		return new MerchantConfig(value, breedNitwitChance, nitwitKillsAffectReputation, offers, maxOffers, levels, currency, prices, slotChances,
			reputationPrices, killTitleChance, screen);
	}

	public MerchantConfig withBreedNitwitChance(double value) {
		return new MerchantConfig(villagers, value, nitwitKillsAffectReputation, offers, maxOffers, levels, currency, prices, slotChances,
			reputationPrices, killTitleChance, screen);
	}

	public MerchantConfig withNitwitKillsAffectReputation(boolean value) {
		return new MerchantConfig(villagers, breedNitwitChance, value, offers, maxOffers, levels, currency, prices, slotChances, reputationPrices, killTitleChance, screen);
	}

	public MerchantConfig withOffers(int value) {
		return new MerchantConfig(villagers, breedNitwitChance, nitwitKillsAffectReputation, value, Math.max(value, maxOffers), levels, currency,
			prices, slotChances, reputationPrices, killTitleChance, screen);
	}

	public MerchantConfig withMaxOffers(int value) {
		return new MerchantConfig(villagers, breedNitwitChance, nitwitKillsAffectReputation, Math.min(offers, value), value, levels, currency,
			prices, slotChances, reputationPrices, killTitleChance, screen);
	}

	public MerchantConfig withPrice(String rarity, int value) {
		Map<String, Integer> changed = new LinkedHashMap<>(prices);
		changed.put(rarity, value);
		return new MerchantConfig(villagers, breedNitwitChance, nitwitKillsAffectReputation, offers, maxOffers, levels, currency, changed,
			slotChances, reputationPrices, killTitleChance, screen);
	}

	public MerchantConfig withScreen(String value) {
		return new MerchantConfig(villagers, breedNitwitChance, nitwitKillsAffectReputation, offers, maxOffers, levels, currency, prices,
			slotChances, reputationPrices, killTitleChance, value);
	}

	public boolean tradingScreen() {
		return !"chest".equals(screen);
	}

	public MerchantConfig withKillTitleChance(double value) {
		return new MerchantConfig(villagers, breedNitwitChance, nitwitKillsAffectReputation, offers, maxOffers, levels, currency, prices,
			slotChances, reputationPrices, value, screen);
	}

	public MerchantConfig withReputationPrices(boolean value) {
		return new MerchantConfig(villagers, breedNitwitChance, nitwitKillsAffectReputation, offers, maxOffers, levels, currency, prices,
			slotChances, value, killTitleChance, screen);
	}
}
