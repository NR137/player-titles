package de.nr137.playertitles.merchant;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import de.nr137.lib.gui.ChestScreen;
import de.nr137.lib.text.TextWidth;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleText;

/** A Title Merchant's screen: the player's offers of the day in the middle row, today's currency at the top. */
public final class MerchantScreen {
	private static final int INFO = 4;

	private MerchantScreen() {
	}

	public static void open(ServerPlayer player, Villager merchant) {
		String language = PlayerTitles.uiLanguage(player);
		Component name = merchant.getCustomName() != null ? merchant.getCustomName() : merchant.getName();
		ChestScreen screen = new ChestScreen(3, PlayerTitles.translations().component(language, "merchant.player_titles.screen", name.getString()));
		draw(screen, player, merchant);
		screen.open(player);
	}

	private static void draw(ChestScreen screen, ServerPlayer player, Villager merchant) {
		String language = PlayerTitles.uiLanguage(player);
		String titleLanguage = PlayerTitles.withForm(PlayerTitles.titleLanguage(player), player);
		long day = TitleMerchants.today();
		Item currency = TitleMerchants.currency(day);
		int carried = TitleMerchants.carried(player, currency);
		TitleData data = TitleData.get(player.level().getServer());
		TitleData.Trades trades = data.trades(player.getUUID());
		screen.clear();
		ItemStack info = new ItemStack(currency);
		info.set(DataComponents.CUSTOM_NAME, plain(text(language, "merchant.player_titles.currency", info.getHoverName().getString()), ChatFormatting.GOLD));
		List<Component> infoLore = new ArrayList<>();
		infoLore.addAll(wrapped(text(language, "merchant.player_titles.carried", carried), ChatFormatting.GRAY));
		int level = TitleMerchants.level(trades.count());
		int next = TitleMerchants.nextLevelAt(trades.count());
		// The level as the game names it (its translation, so in the player's language on their client).
		MutableComponent levelLine = text(language, "merchant.player_titles.level").append(Component.translatable(TitleMerchants.LEVEL_KEYS.get(level - 1)));
		infoLore.add(plain(levelLine, ChatFormatting.AQUA));
		infoLore.addAll(wrapped(next < 0 ? text(language, "merchant.player_titles.max_level", trades.count())
			: text(language, "merchant.player_titles.next_level", trades.count(), next, next - trades.count()), ChatFormatting.GRAY));
		int percent = (int) Math.round((TitleMerchants.priceFactor(player, merchant) - 1.0) * 100);
		if (percent != 0) {
			infoLore.addAll(wrapped(text(language, percent > 0 ? "merchant.player_titles.dearer" : "merchant.player_titles.cheaper", Math.abs(percent)),
				percent > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
		}
		infoLore.addAll(wrapped(text(language, "merchant.player_titles.tomorrow"), ChatFormatting.DARK_GRAY));
		info.set(DataComponents.LORE, new ItemLore(infoLore));
		// Only for showing: no food values, so mods that show them (hunger icons) don't.
		info.remove(DataComponents.FOOD);
		info.remove(DataComponents.CONSUMABLE);
		screen.set(INFO, info, null);
		List<Title> offers = TitleMerchants.offers(player, day);
		List<String> bought = trades.boughtOn(day);
		List<Integer> slots = layout(offers.size());
		for (int i = 0; i < offers.size(); i++) {
			Title title = offers.get(i);
			boolean sold = bought.contains(title.id());
			int price = TitleMerchants.price(player, merchant, title);
			ItemStack item = new ItemStack(sold ? Items.PAPER : Items.NAME_TAG);
			item.set(DataComponents.CUSTOM_NAME, TitleText.of(title, titleLanguage, 0).withStyle(style -> style.withItalic(false)));
			List<Component> lore = new ArrayList<>();
			String description = title.description(titleLanguage);
			if (!description.isEmpty()) {
				lore.addAll(wrapped(Component.literal(description), ChatFormatting.GRAY));
			}
			lore.addAll(wrapped(text(language, "rarity.player_titles." + (title.rarity() == null ? "common" : title.rarity())), ChatFormatting.DARK_AQUA));
			if (sold) {
				lore.addAll(wrapped(text(language, "merchant.player_titles.bought"), ChatFormatting.GREEN));
			} else {
				lore.addAll(wrapped(text(language, "merchant.player_titles.price", price, new ItemStack(currency).getHoverName().getString()),
					carried >= price ? ChatFormatting.YELLOW : ChatFormatting.RED));
				lore.addAll(wrapped(text(language, carried >= price ? "merchant.player_titles.click" : "merchant.player_titles.not_enough"),
					carried >= price ? ChatFormatting.YELLOW : ChatFormatting.RED));
			}
			item.set(DataComponents.LORE, new ItemLore(lore));
			screen.set(slots.get(i), item, sold ? null : click -> {
				if (TitleMerchants.buy(click.player(), merchant, title)) {
					click.player().level().playSound(null, click.player().blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
					click.player().sendSystemMessage(PlayerTitles.translations().component(PlayerTitles.uiLanguage(click.player()), "message.player_titles.got")
						.append(TitleText.of(title, PlayerTitles.withForm(PlayerTitles.titleLanguage(click.player()), click.player()), 0)));
				} else {
					click.player().level().playSound(null, click.player().blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
				}
				draw(screen, click.player(), merchant);
			});
		}
		if (offers.isEmpty()) {
			ItemStack none = new ItemStack(Items.BARRIER);
			none.set(DataComponents.CUSTOM_NAME, plain(text(language, "merchant.player_titles.none"), ChatFormatting.WHITE));
			screen.set(13, none, null);
		}
	}

	/** Symmetric: 3 in a row; 3 and 1 below the middle; 5 in a row; 6 as 3 and 3; 7 in a row (rows 2 and 3). */
	private static List<Integer> layout(int count) {
		return switch (count) {
			case 1 -> List.of(13);
			case 2 -> List.of(12, 14);
			case 3 -> List.of(12, 13, 14);
			case 4 -> List.of(12, 13, 14, 22);
			case 5 -> List.of(11, 12, 13, 14, 15);
			case 6 -> List.of(12, 13, 14, 21, 22, 23);
			case 7 -> List.of(10, 11, 12, 13, 14, 15, 16);
			default -> java.util.stream.IntStream.range(0, Math.min(count, 18)).map(i -> 9 + i).boxed().toList();
		};
	}

	private static List<Component> wrapped(Component text, ChatFormatting color) {
		return TextWidth.wrap(text.getString(), TextWidth.TOOLTIP).stream()
			.<Component>map(line -> plain(Component.literal(line), color)).toList();
	}

	private static MutableComponent plain(MutableComponent text, ChatFormatting color) {
		return text.withStyle(style -> style.withItalic(false).withColor(color));
	}

	private static MutableComponent text(String language, String key, Object... args) {
		return PlayerTitles.translations().component(language, key, args);
	}
}
