package de.nr137.playertitles.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleText;

/**
 * Title name tags: name tags found in loot chests carry our mark ({@code custom_data}). Using one (right-click into
 * the air) collects a random title the player doesn't have yet, from those with the source "loot"; some are blank
 * (the first blank gives its own title). Once a player has them all, a name tag is blank for sure until they have the
 * blank's title, then likelier blank until it gives the title for having them all; after that it can't be used. The name, the lore
 * and the glint are added only in the packets (ClientItems), so without the mod it is an ordinary name tag.
 */
public final class TitleTags {
	private static final String KEY = PlayerTitles.MOD_ID;
	private static final String MARK = "title_tag";
	public static final String SOURCE = "loot";
	/** The title for the first blank. */
	public static final String BLANK_SOURCE = "loot_blank";
	/** The title for having every title with the source "loot": from the next title name tag. */
	public static final String COMPLETE_SOURCE = "loot_complete";
	private static final Map<String, Integer> WEIGHTS = Map.of("common", 60, "rare", 25, "epic", 12, "legendary", 3);

	private TitleTags() {
	}

	public static boolean isTitleTag(ItemStack stack) {
		if (!stack.is(Items.NAME_TAG)) {
			return false;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getCompoundOrEmpty(KEY).getBooleanOr(MARK, false);
	}

	public static void mark(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag ours = tag.getCompoundOrEmpty(KEY);
			ours.putBoolean(MARK, true);
			tag.put(KEY, ours);
		});
	}

	/** Each loot table's id, for the chances per table (the game's loot tables don't know their own). */
	private static final Map<LootTable, String> TABLE_IDS = new java.util.IdentityHashMap<>();
	private static Object tablesOf;

	/** A loot chest was filled (LootTableMixin): its name tags become title name tags, by the table's chance. */
	public static void onLootFilled(Container container, LootTable table, LootParams params) {
		markMaybe(container, PlayerTitles.config().loot().markChance(tableId(table, params.getLevel().getServer())), params.getLevel().getRandom());
	}

	/** The name tags in a container become title name tags, each with this chance. */
	public static void markMaybe(Container container, double chance, net.minecraft.util.RandomSource random) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			markMaybe(container.getItem(i), chance, random);
		}
	}

	/** A catch (FishingHookMixin): its name tags become title name tags, by the fishing chance. */
	public static void onFished(List<ItemStack> items, net.minecraft.util.RandomSource random) {
		double chance = PlayerTitles.config().loot().markChance(LootConfig.FISHING);
		items.forEach(stack -> markMaybe(stack, chance, random));
	}

	private static void markMaybe(ItemStack stack, double chance, net.minecraft.util.RandomSource random) {
		if (stack.is(Items.NAME_TAG) && !stack.has(DataComponents.CUSTOM_NAME) && !isTitleTag(stack) && random.nextDouble() < chance) {
			mark(stack);
		}
	}

	private static synchronized String tableId(LootTable table, net.minecraft.server.MinecraftServer server) {
		Object tables = server.reloadableRegistries();
		if (tables != tablesOf) {
			TABLE_IDS.clear();
			server.reloadableRegistries().lookup().lookupOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE).listElements()
				.forEach(holder -> TABLE_IDS.put(holder.value(), holder.key().identifier().toString()));
			tablesOf = tables;
		}
		return TABLE_IDS.getOrDefault(table, "");
	}

	/** As clients see a title name tag: its own name (unless renamed), what it does, a glint; in the viewer's language. */
	public static ItemStack forClient(ItemStack stack, @org.jspecify.annotations.Nullable ServerPlayer viewer) {
		if (!isTitleTag(stack)) {
			return stack;
		}
		ItemStack shown = stack.copy();
		String language = viewer == null ? PlayerTitles.fallbackLanguage() : PlayerTitles.uiLanguage(viewer);
		shown.set(DataComponents.ITEM_NAME, PlayerTitles.translations().component(language, "item.player_titles.title_tag"));
		shown.set(DataComponents.LORE, new ItemLore(List.of(PlayerTitles.translations().component(language, "item.player_titles.title_tag.lore")
			.withStyle(style -> style.withItalic(false).withColor(net.minecraft.ChatFormatting.GRAY)))));
		shown.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return shown;
	}

	/** A title name tag renamed in an anvil, used on a mob (NameTagItemMixin): it doesn't name it. */
	public static boolean blockNaming(net.minecraft.world.entity.player.Player player, ItemStack stack) {
		if (!isTitleTag(stack)) {
			return false;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(PlayerTitles.translations().component(PlayerTitles.uiLanguage(serverPlayer), "message.player_titles.tag_no_naming"));
		}
		return true;
	}

	/**
	 * Using a title name tag (ItemMixin): a random new title, or with blankChance nothing; the tag is used up. False if
	 * there is no title left to get (the tag stays).
	 */
	public static boolean use(ServerPlayer player, ItemStack stack) {
		TitleData data = TitleData.get(player.level().getServer());
		List<Title> pool = new ArrayList<>();
		Title dud = null;
		Title completion = null;
		for (Title title : PlayerTitles.titles().all()) {
			if (title.sourcesOrNone().contains(SOURCE) && !data.owns(player.getUUID(), title.id())) {
				pool.add(title);
			}
			if (title.sourcesOrNone().contains(BLANK_SOURCE)) {
				dud = title;
			}
			if (title.sourcesOrNone().contains(COMPLETE_SOURCE)) {
				completion = title;
			}
		}
		String language = PlayerTitles.uiLanguage(player);
		boolean haveDud = dud == null || data.owns(player.getUUID(), dud.id());
		boolean haveCompletion = completion == null || data.owns(player.getUUID(), completion.id());
		LootConfig config = PlayerTitles.config().loot();
		boolean blank;
		if (!pool.isEmpty()) {
			blank = player.getRandom().nextDouble() < config.blankChance();
		} else if (!haveDud) {
			// Every random title but the one for a blank: this one is blank, for sure.
			blank = true;
		} else if (!haveCompletion) {
			// Only the title for having them all is missing: blanks are much likelier now.
			blank = player.getRandom().nextDouble() < config.lastBlankChance();
		} else {
			player.sendSystemMessage(PlayerTitles.translations().component(language, "message.player_titles.tag_empty"));
			return false;
		}
		stack.consume(1, player);
		if (blank) {
			player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.2F);
			player.sendSystemMessage(PlayerTitles.translations().component(language, "message.player_titles.tag_blank"));
			// The first blank gives its own title.
			if (dud != null && data.give(player.getUUID(), dud.id(), BLANK_SOURCE)) {
				announce(player, dud, language);
			}
			return true;
		}
		Title chosen = pool.isEmpty() ? completion : pick(pool, player);
		data.give(player.getUUID(), chosen.id(), pool.isEmpty() ? COMPLETE_SOURCE : SOURCE);
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
		announce(player, chosen, language);
		return true;
	}

	private static Title pick(List<Title> pool, ServerPlayer player) {
		int total = pool.stream().mapToInt(TitleTags::weight).sum();
		int pick = player.getRandom().nextInt(total);
		for (Title title : pool) {
			pick -= weight(title);
			if (pick < 0) {
				return title;
			}
		}
		return pool.get(0);
	}

	private static void announce(ServerPlayer player, Title title, String language) {
		player.sendSystemMessage(PlayerTitles.translations().component(language, "message.player_titles.got")
			.append(TitleText.of(title, PlayerTitles.withForm(PlayerTitles.titleLanguage(player), player), 0)));
	}

	private static int weight(Title title) {
		return WEIGHTS.getOrDefault(title.rarity() == null ? "common" : title.rarity().toLowerCase(Locale.ROOT), 60);
	}
}
