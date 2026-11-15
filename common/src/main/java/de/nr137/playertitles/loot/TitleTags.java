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
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleText;

/**
 * Title name tags: name tags found in loot chests carry our mark ({@code custom_data}). Using one (right-click into
 * the air) collects a random title the player doesn't have yet, from those with the source "loot". The name, the lore
 * and the glint are added only in the packets (ClientItems), so without the mod it is an ordinary name tag.
 */
public final class TitleTags {
	private static final String KEY = PlayerTitles.MOD_ID;
	private static final String MARK = "title_tag";
	public static final String SOURCE = "loot";
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

	/** A loot chest was filled (LootTableMixin): its name tags become title name tags. */
	public static void onLootFilled(Container container) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (stack.is(Items.NAME_TAG) && !stack.has(DataComponents.CUSTOM_NAME) && !isTitleTag(stack)) {
				mark(stack);
			}
		}
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

	/** Using a title name tag (ItemMixin): a random new title; the tag is used up. False if there is none to get. */
	public static boolean use(ServerPlayer player, ItemStack stack) {
		TitleData data = TitleData.get(player.level().getServer());
		List<Title> pool = new ArrayList<>();
		for (Title title : PlayerTitles.titles().all()) {
			if (title.sourcesOrNone().contains(SOURCE) && !data.owns(player.getUUID(), title.id())) {
				pool.add(title);
			}
		}
		String language = PlayerTitles.uiLanguage(player);
		if (pool.isEmpty()) {
			player.sendSystemMessage(PlayerTitles.translations().component(language, "message.player_titles.tag_empty"));
			return false;
		}
		int total = pool.stream().mapToInt(TitleTags::weight).sum();
		int pick = player.getRandom().nextInt(total);
		Title chosen = pool.get(0);
		for (Title title : pool) {
			pick -= weight(title);
			if (pick < 0) {
				chosen = title;
				break;
			}
		}
		data.give(player.getUUID(), chosen.id(), SOURCE);
		stack.consume(1, player);
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
		player.sendSystemMessage(PlayerTitles.translations().component(language, "message.player_titles.got")
			.append(TitleText.of(chosen, PlayerTitles.withForm(PlayerTitles.titleLanguage(player), player), 0)));
		return true;
	}

	private static int weight(Title title) {
		return WEIGHTS.getOrDefault(title.rarity() == null ? "common" : title.rarity().toLowerCase(Locale.ROOT), 60);
	}
}
