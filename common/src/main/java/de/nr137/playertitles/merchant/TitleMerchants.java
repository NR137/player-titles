package de.nr137.playertitles.merchant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.title.Title;

/**
 * Title Merchants: a villager (nitwit or unemployed, see MerchantConfig) becomes one by picking up a dropped name tag.
 * It holds the name tag. A villager without a name gets one: the name tag's (renamed in an anvil), or one of ours. It
 * is the villager's own, vanilla name, which a name tag can change later; above it the merchant shows the title "Title
 * Merchant" (MerchantBadges). Which villagers are merchants is kept in the mod's file, so without the mod it is an
 * ordinary villager with a name holding a name tag.
 * <p>
 * Every player gets their own offers, the same at every merchant, new every day (the server's date): titles they don't
 * have, from those with the source "merchant", the rarer ones less often. They pay with the day's currency item.
 */
public final class TitleMerchants {
	/** Names for merchants whose name tag has none: from many languages, the same everywhere. */
	private static final List<String> NAMES = List.of("Ambrose", "Bertram", "Cornelius", "Dagobert", "Eustace", "Fridolin",
		"Gideon", "Hieronymus", "Ignatius", "Jasper", "Kasimir", "Leopold", "Mortimer", "Nepomuk", "Octavius", "Ottilie",
		"Pankraz", "Quirin", "Rosalind", "Sieglinde", "Theodora", "Ulrich", "Valentin", "Wilhelmina", "Xaver", "Zacharias");
	public static final String SOURCE = "merchant";

	private TitleMerchants() {
	}

	public static MerchantConfig config() {
		return PlayerTitles.config().merchant();
	}

	/** Whether a villager would become a merchant by picking up a name tag. */
	public static boolean canBecome(Villager villager) {
		if (villager.isBaby() || TitleData.get(villager.level().getServer()).isMerchant(villager.getUUID())) {
			return false;
		}
		var profession = villager.getVillagerData().profession();
		return config().nitwitsCan() && profession.is(VillagerProfession.NITWIT)
			|| config().unemployedCan() && profession.is(VillagerProfession.NONE);
	}

	public static boolean isMerchant(Villager villager) {
		return villager.level().getServer() != null && TitleData.get(villager.level().getServer()).isMerchant(villager.getUUID());
	}

	/** A villager picks up a name tag (VillagerMixin): it becomes a merchant, unless the player who dropped it may not. */
	public static boolean pickUp(ServerLevel level, Villager villager, ItemEntity entity) {
		if (!entity.getItem().is(Items.NAME_TAG) || !canBecome(villager)
				|| entity.getOwner() instanceof ServerPlayer thrower && !PlayerTitles.MERCHANT.check(thrower)) {
			return false;
		}
		ItemStack tag = entity.getItem().split(1);
		if (entity.getItem().isEmpty()) {
			entity.discard();
		}
		Component name = tag.get(DataComponents.CUSTOM_NAME);
		villager.setItemSlot(EquipmentSlot.MAINHAND, tag);
		if (!villager.hasCustomName()) {
			villager.setCustomName(name != null ? name : Component.literal(NAMES.get(Math.floorMod(villager.getUUID().hashCode(), NAMES.size()))));
		}
		villager.setPersistenceRequired();
		TitleData.get(level.getServer()).setMerchant(villager.getUUID(), true);
		MerchantBadges.refresh(villager);
		villager.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
		return true;
	}

	public static final String KILL_SOURCE = "merchant_kill";

	/**
	 * A merchant is dying (VillagerMixin, before its drops): it drops nothing it holds, and its killer may get the
	 * secret title (killTitleChance).
	 */
	public static void onDying(Villager villager, net.minecraft.world.damagesource.DamageSource source) {
		if (!isMerchant(villager)) {
			return;
		}
		var accessor = (de.nr137.playertitles.mixin.MobAccessor) villager;
		accessor.playerTitles$setDropChances(accessor.playerTitles$dropChances()
			.withEquipmentChance(EquipmentSlot.MAINHAND, 0.0F).withEquipmentChance(EquipmentSlot.OFFHAND, 0.0F));
		if (source.getEntity() instanceof ServerPlayer killer && villager.getRandom().nextDouble() < killChance(villager, source)) {
			TitleData data = TitleData.get(killer.level().getServer());
			for (Title title : PlayerTitles.titles().all()) {
				if (title.sourcesOrNone().contains(KILL_SOURCE) && data.give(killer.getUUID(), title.id(), KILL_SOURCE)) {
					killer.sendSystemMessage(PlayerTitles.translations().component(PlayerTitles.uiLanguage(killer), "message.player_titles.got")
						.append(de.nr137.playertitles.title.TitleText.of(title, PlayerTitles.withForm(PlayerTitles.titleLanguage(killer), killer), 0)));
				}
			}
		}
	}

	/** killTitleChance, and as with the game's rare drops 1 % more per Looting level of the weapon that killed it. */
	private static double killChance(Villager villager, net.minecraft.world.damagesource.DamageSource source) {
		ItemStack weapon = source.getWeaponItem();
		int looting = weapon == null ? 0 : net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
			villager.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), weapon);
		return config().killTitleChance() + 0.01 * looting;
	}

	public static void onDeath(Villager villager) {
		if (villager.level().getServer() != null) {
			TitleData.get(villager.level().getServer()).setMerchant(villager.getUUID(), false);
			MerchantBadges.remove(villager);
		}
	}

	/** Days added on dev servers (/titles dev nextday), to try out a new day; 0 elsewhere. */
	private static long devDays;

	/** Today, as the server's date (the real one): offers and the currency change at midnight. */
	public static long today() {
		return LocalDate.now().toEpochDay() + devDays;
	}

	/** Dev servers only: the merchants act as if it were the next day. */
	public static void devNextDay() {
		devDays++;
	}

	/**
	 * A mob became another (MobMixin): a Title Merchant turned zombie villager stays one, and is one again when cured,
	 * as a villager keeps its job, trades and gossip (the mark moves to the new entity, which has a new id).
	 */
	public static void onConverted(net.minecraft.world.entity.Mob from, net.minecraft.world.entity.@org.jspecify.annotations.Nullable Mob to) {
		if (to == null || from.level().getServer() == null) {
			return;
		}
		boolean pair = from instanceof Villager && to instanceof net.minecraft.world.entity.monster.zombie.ZombieVillager
			|| from instanceof net.minecraft.world.entity.monster.zombie.ZombieVillager && to instanceof Villager;
		TitleData data = TitleData.get(from.level().getServer());
		if (pair && data.isMerchant(from.getUUID())) {
			data.setMerchant(to.getUUID(), true);
			if (from instanceof net.minecraft.world.entity.monster.zombie.ZombieVillager) {
				data.setMerchant(from.getUUID(), false);
			}
		}
	}

	/** The item titles cost today (MerchantConfig.currency, in turn). */
	public static Item currency(long day) {
		List<String> list = config().currency() == null ? List.of() : List.copyOf(config().currency().keySet());
		if (list.isEmpty()) {
			return Items.ROTTEN_FLESH;
		}
		String id = list.get((int) Math.floorMod(day, list.size()));
		return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(Items.ROTTEN_FLESH);
	}

	/** A rarity's price in today's currency (before reputation). */
	public static int basePrice(Title title, long day) {
		return config().price(title.rarity(), BuiltInRegistries.ITEM.getKey(currency(day)).toString());
	}

	/** A player's offers today: always the same for them on one day, new the next. */
	public static List<Title> offers(ServerPlayer player, long day) {
		TitleData data = TitleData.get(player.level().getServer());
		TitleData.Trades trades = data.trades(player.getUUID());
		List<String> boughtToday = trades.boughtOn(day);
		List<Title> pool = new ArrayList<>();
		for (Title title : PlayerTitles.titles().all()) {
			if (title.sourcesOrNone().contains(SOURCE) && (!data.owns(player.getUUID(), title.id()) || boughtToday.contains(title.id()))) {
				pool.add(title);
			}
		}
		// Today's choice for this player; bought ones stay in it (shown as bought) until tomorrow.
		Random random = new Random(player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits() * 31 ^ day * 1_000_003L);
		// Titles bought today count too: a level reached unlocks its offer right away (the others stay as they were).
		int count = Math.min(config().offersAfter(trades.count()), pool.size());
		List<Title> offers = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			// Each offer has its own chances (MerchantConfig.slotChances): first the rarity, among those the player has
			// titles of left, then one of its titles.
			Map<String, Integer> chances = config().chancesFor(i);
			Map<String, List<Title>> byRarity = new java.util.LinkedHashMap<>();
			for (Title title : pool) {
				byRarity.computeIfAbsent(rarity(title), ignored -> new ArrayList<>()).add(title);
			}
			int total = byRarity.keySet().stream().mapToInt(rarity -> chances.getOrDefault(rarity, 0)).sum();
			List<Title> from = pool;
			if (total > 0) {
				int pick = random.nextInt(total);
				for (Map.Entry<String, List<Title>> entry : byRarity.entrySet()) {
					pick -= chances.getOrDefault(entry.getKey(), 0);
					if (pick < 0) {
						from = entry.getValue();
						break;
					}
				}
			}
			Title chosen = from.get(random.nextInt(from.size()));
			offers.add(chosen);
			pool.remove(chosen);
		}
		return offers;
	}

	private static String rarity(Title title) {
		return title.rarity() == null ? "common" : title.rarity().toLowerCase(Locale.ROOT);
	}

	/** The game's names of the merchant levels, Novice to Master (as for villagers). */
	public static final List<String> LEVEL_KEYS = List.of("merchant.level.1", "merchant.level.2", "merchant.level.3", "merchant.level.4",
		"merchant.level.5");

	/** The player's merchant level, 1 (Novice) to 5 (Master): one more for each of merchant.levels reached. */
	public static int level(int trades) {
		int level = 1;
		for (int needed : config().levels()) {
			if (trades >= needed && level < LEVEL_KEYS.size()) {
				level++;
			}
		}
		return level;
	}

	/** Titles bought needed for the next level, or -1 at the highest. */
	public static int nextLevelAt(int trades) {
		int level = level(trades);
		return level >= LEVEL_KEYS.size() || level - 1 >= config().levels().size() ? -1 : config().levels().get(level - 1);
	}

	/** How many of the currency the player carries. */
	public static int carried(ServerPlayer player, Item currency) {
		return player.getInventory().clearOrCountMatchingItems(stack -> stack.is(currency), true, Integer.MAX_VALUE,
			player.inventoryMenu.getCraftSlots());
	}

	/**
	 * What a title costs this player at this merchant: its rarity's price, changed by the merchant's gossip about them
	 * (as vanilla villagers' prices are: one hit 25 % dearer, trading and curing cheaper) and the Hero of the Village
	 * discount, from half to three times; at least 1.
	 */
	public static int price(ServerPlayer player, Villager merchant, Title title) {
		return Math.max(1, (int) Math.round(basePrice(title, today()) * priceFactor(player, merchant)));
	}

	/** The factor on the prices (1: none), for showing it. */
	public static double priceFactor(ServerPlayer player, Villager merchant) {
		if (!config().reputationPrices()) {
			return 1.0;
		}
		double factor = Math.clamp(1.0 - merchant.getPlayerReputation(player) / 100.0, 0.5, 3.0);
		var hero = player.getEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE);
		if (hero != null) {
			factor *= 1.0 - Math.min(0.9, 0.3 + 0.0625 * hero.getAmplifier());
		}
		return Math.max(0.1, factor);
	}

	/** Buys an offer: pays, gets the title, one more trade. False if they can't pay or it isn't on offer. */
	public static boolean buy(ServerPlayer player, Villager merchant, Title title) {
		long day = today();
		if (!PlayerTitles.MERCHANT.check(player) || !offers(player, day).contains(title)) {
			return false;
		}
		TitleData data = TitleData.get(player.level().getServer());
		Item currency = currency(day);
		int price = price(player, merchant, title);
		if (data.owns(player.getUUID(), title.id()) || carried(player, currency) < price) {
			return false;
		}
		player.getInventory().clearOrCountMatchingItems(stack -> stack.is(currency), false, price, player.inventoryMenu.getCraftSlots());
		data.give(player.getUUID(), title.id(), SOURCE);
		data.bought(player.getUUID(), title.id(), day);
		// A trade, as with any villager: good gossip, so lower prices over time.
		if (merchant.level() instanceof ServerLevel level) {
			level.onReputationEvent(net.minecraft.world.entity.ai.village.ReputationEventType.TRADE, player, merchant);
		}
		return true;
	}

	public static UUID id(Villager villager) {
		return villager.getUUID();
	}
}
