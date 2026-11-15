package de.nr137.playertitles.screen;

import java.text.Collator;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import de.nr137.lib.dialog.Dialogs;
import de.nr137.lib.gui.ChestScreen;
import de.nr137.lib.text.TextWidth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.TitleActions;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleText;

/**
 * /titles: a chest screen with the player's titles, 45 per page. Clicking a title wears it; the bottom row has the
 * pages, search and sorting (a dialog; the hopper switches the sorting right away), taking the title off and animated
 * titles on or off. Sorting and search last until the server stops.
 */
public final class TitlesScreen {
	private static final int PER_PAGE = 45;
	private static final int PREVIOUS = 45;
	private static final int OFF = 49;
	private static final int ANIMATIONS = 51;
	private static final int SEARCH = 46;
	private static final int SORT = 47;
	private static final int FORM = 50;
	private static final int CATEGORY = 48;
	/** The category with every title. */
	private static final String ALL = "all";
	private static final List<String> RARITIES = List.of("legendary", "epic", "rare", "common");

	/** How a player's list is sorted. */
	public enum Sort {
		/** Newest first. */
		DATE,
		/** Most precious first. */
		RARITY,
		/** By the title's text (without icons and side letters). */
		NAME
	}

	private record View(Sort sort, String search, String category) {
	}

	private static final Map<UUID, View> VIEWS = new HashMap<>();
	private static final int NEXT = 53;

	private TitlesScreen() {
	}

	/** Opens the screen showing only titles whose text or description contains {@code search} ("" for all). */
	public static void search(ServerPlayer player, String search) {
		VIEWS.put(player.getUUID(), new View(view(player).sort(), search.trim(), view(player).category()));
		open(player, 0);
	}

	private static View view(ServerPlayer player) {
		return VIEWS.getOrDefault(player.getUUID(), new View(Sort.DATE, "", ALL));
	}

	public static void open(ServerPlayer player, int page) {
		ChestScreen screen = new ChestScreen(6, Component.empty());
		draw(screen, player, page);
		screen.open(player);
	}

	private static void draw(ChestScreen screen, ServerPlayer player, int requestedPage) {
		String language = PlayerTitles.uiLanguage(player);
		String titleLanguage = PlayerTitles.withForm(PlayerTitles.titleLanguage(player), player);
		TitleData.PlayerRecord record = TitleData.get(player.level().getServer()).of(player.getUUID());
		View view = view(player);
		String search = view.search().toLowerCase(Locale.ROOT);
		List<String> categories = categories(record);
		String category = categories.contains(view.category()) ? view.category() : ALL;
		List<Map.Entry<Title, TitleData.Owned>> owned = new ArrayList<>();
		for (Map.Entry<String, TitleData.Owned> entry : record.owned().entrySet()) {
			PlayerTitles.titles().get(entry.getKey())
				.filter(title -> category.equals(ALL) || category.equals(title.category()))
				.filter(title -> search.isEmpty() || title.text(titleLanguage).toLowerCase(Locale.ROOT).contains(search)
					|| title.description(titleLanguage).toLowerCase(Locale.ROOT).contains(search))
				.ifPresent(title -> owned.add(Map.entry(title, entry.getValue())));
		}
		Collator collator = Collator.getInstance(locale(titleLanguage));
		Comparator<Map.Entry<Title, TitleData.Owned>> byName = Comparator.comparing(entry -> entry.getKey().text(titleLanguage), collator);
		owned.sort(switch (view.sort()) {
			case DATE -> Comparator.<Map.Entry<Title, TitleData.Owned>>comparingLong(entry -> -entry.getValue().time()).thenComparing(byName);
			case RARITY -> Comparator.<Map.Entry<Title, TitleData.Owned>>comparingInt(entry -> rarity(entry.getKey())).thenComparing(byName);
			case NAME -> byName;
		});
		int pages = Math.max(1, (owned.size() + PER_PAGE - 1) / PER_PAGE);
		int page = Math.clamp(requestedPage, 0, pages - 1);
		screen.clear().title(text(language, "screen.player_titles.title", owned.size(), page + 1, pages));
		Optional<String> worn = record.worn();
		for (int i = 0; i < PER_PAGE && page * PER_PAGE + i < owned.size(); i++) {
			Map.Entry<Title, TitleData.Owned> entry = owned.get(page * PER_PAGE + i);
			Title title = entry.getKey();
			boolean wearing = worn.filter(title.id()::equals).isPresent();
			screen.set(i, titleItem(title, entry.getValue(), wearing, language, titleLanguage), click -> {
				if (wearing) {
					TitleActions.takeOff(click.player());
				} else {
					TitleActions.wear(click.player(), title.id());
				}
				draw(screen, click.player(), page);
			});
		}
		if (owned.isEmpty()) {
			screen.set(22, search.isEmpty() ? button(Items.PAPER, text(language, "screen.player_titles.none"), text(language, "screen.player_titles.none.hint"))
				: button(Items.PAPER, text(language, "screen.player_titles.no_match", view.search()), null), null);
		}
		screen.set(SEARCH, button(Items.SPYGLASS, view.search().isEmpty() ? text(language, "screen.player_titles.search")
			: text(language, "screen.player_titles.searching", view.search()), text(language, view.search().isEmpty()
				? "screen.player_titles.search.hint" : "screen.player_titles.searching.hint")), click -> {
				if (click.right()) {
					search(click.player(), "");
				} else {
					click.player().closeContainer();
					openSearch(click.player());
				}
			});
		if (categories.size() > 2) {
			// "All" and at least two categories of titles the player has: switch between them.
			screen.set(CATEGORY, button(Items.BOOKSHELF, text(language, "screen.player_titles.category", categoryName(category, language)),
				text(language, "screen.player_titles.category.hint")), click -> {
					int step = click.right() ? categories.size() - 1 : 1;
					String next = categories.get((categories.indexOf(category) + step) % categories.size());
					VIEWS.put(click.player().getUUID(), new View(view.sort(), view.search(), next));
					draw(screen, click.player(), 0);
				});
		}
		screen.set(SORT, button(Items.HOPPER, text(language, "screen.player_titles.sort." + view.sort().name().toLowerCase(Locale.ROOT)),
			text(language, "screen.player_titles.sort.hint")), click -> {
				Sort next = Sort.values()[(view.sort().ordinal() + 1) % Sort.values().length];
				VIEWS.put(click.player().getUUID(), new View(next, view.search(), view.category()));
				draw(screen, click.player(), 0);
			});
		if (page > 0) {
			screen.set(PREVIOUS, button(Items.ARROW, text(language, "screen.player_titles.previous"), null),
				click -> draw(screen, click.player(), page - 1));
		}
		if (page < pages - 1) {
			screen.set(NEXT, button(Items.ARROW, text(language, "screen.player_titles.next"), null),
				click -> draw(screen, click.player(), page + 1));
		}
		String baseLanguage = PlayerTitles.titleLanguage(player);
		if (PlayerTitles.titles().hasForms(baseLanguage)) {
			String form = record.form().orElse(PlayerTitles.config().defaultForm());
			screen.set(FORM, button(Items.NAME_TAG, text(language, "screen.player_titles.form", text(language, "form.player_titles." + form).getString()),
				text(language, "screen.player_titles.form.hint")), click -> {
					TitleActions.setForm(click.player(), Title.FORMS.get((Title.FORMS.indexOf(form) + 1) % Title.FORMS.size()));
					draw(screen, click.player(), page);
				});
		}
		boolean animations = record.animations();
		screen.set(ANIMATIONS, button(Items.CLOCK, text(language, animations ? "screen.player_titles.animations.on" : "screen.player_titles.animations.off"),
			text(language, "screen.player_titles.animations.hint")), click -> {
				TitleActions.setAnimations(click.player(), !animations);
				draw(screen, click.player(), page);
			});
		if (worn.isPresent()) {
			screen.set(OFF, button(Items.BARRIER, text(language, "screen.player_titles.off"), null), click -> {
				TitleActions.takeOff(click.player());
				draw(screen, click.player(), page);
			});
		}
	}

	private static ItemStack titleItem(Title title, TitleData.Owned owned, boolean wearing, String language, String titleLanguage) {
		ItemStack item = new ItemStack(Items.NAME_TAG);
		item.set(DataComponents.CUSTOM_NAME, TitleText.of(title, titleLanguage, 0).withStyle(style -> style.withItalic(false)));
		List<Component> lore = new ArrayList<>();
		String description = title.description(titleLanguage);
		if (!description.isEmpty()) {
			lore.addAll(wrapped(Component.literal(description), ChatFormatting.GRAY));
		}
		lore.addAll(wrapped(text(language, "screen.player_titles.from", source(owned.source(), language), date(owned.time(), language)),
			ChatFormatting.DARK_GRAY));
		lore.addAll(wrapped(text(language, wearing ? "screen.player_titles.wearing" : "screen.player_titles.click"),
			wearing ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
		item.set(DataComponents.LORE, new ItemLore(lore));
		if (wearing) {
			item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return item;
	}

	private static ItemStack button(net.minecraft.world.item.Item type, Component name, Component hint) {
		ItemStack item = new ItemStack(type);
		item.set(DataComponents.CUSTOM_NAME, plain(name.copy(), ChatFormatting.WHITE));
		if (hint != null) {
			item.set(DataComponents.LORE, new ItemLore(wrapped(hint, ChatFormatting.GRAY)));
		}
		return item;
	}

	private static String source(String source, String language) {
		String kind = source.contains(":") ? source.substring(0, source.indexOf(':')) : source;
		return PlayerTitles.translations().text(language, "source.player_titles." + kind);
	}

	/** "All", then the categories of the titles the player has, in the library's order. */
	private static List<String> categories(TitleData.PlayerRecord record) {
		List<String> result = new ArrayList<>(List.of(ALL));
		for (String category : PlayerTitles.titles().categories()) {
			boolean has = record.owned().keySet().stream()
				.anyMatch(id -> PlayerTitles.titles().get(id).map(title -> category.equals(title.category())).orElse(false));
			if (has) {
				result.add(category);
			}
		}
		return result;
	}

	private static String categoryName(String category, String language) {
		return PlayerTitles.titles().categoryName(category, language);
	}

	/**
	 * The search dialog: a text field, the sorting and the category; "Search" opens the screen with them, "Show all"
	 * without search, in every category.
	 */
	public static void openSearch(ServerPlayer player) {
		String language = PlayerTitles.uiLanguage(player);
		View view = view(player);
		List<SingleOptionInput.Entry> sorts = new ArrayList<>();
		for (Sort sort : Sort.values()) {
			String key = sort.name().toLowerCase(Locale.ROOT);
			sorts.add(new SingleOptionInput.Entry(key, Optional.of(text(language, "dialog.player_titles.sort." + key)), sort == view.sort()));
		}
		List<SingleOptionInput.Entry> categoryEntries = new ArrayList<>();
		for (String category : categories(TitleData.get(player.level().getServer()).of(player.getUUID()))) {
			categoryEntries.add(new SingleOptionInput.Entry(category, Optional.of(Component.literal(categoryName(category, language))),
				category.equals(view.category())));
		}
		List<Input> inputs = new ArrayList<>(List.of(
			new Input("search", new TextInput(200, text(language, "dialog.player_titles.search"), true, view.search(), 64, Optional.empty())),
			new Input("sort", new SingleOptionInput(200, sorts, text(language, "dialog.player_titles.sort"), true))));
		if (categoryEntries.size() > 2) {
			inputs.add(new Input("category", new SingleOptionInput(200, categoryEntries, text(language, "dialog.player_titles.category"), true)));
		}
		CommonDialogData common = new CommonDialogData(text(language, "dialog.player_titles.title"), Optional.empty(), true, false,
			DialogAction.CLOSE, List.of(), inputs);
		ActionButton show = new ActionButton(new CommonButtonData(text(language, "dialog.player_titles.show"), Optional.empty(), 100),
			Optional.of(new CustomAll(SEARCH_ACTION, Optional.empty())));
		CompoundTag all = new CompoundTag();
		all.putBoolean("all", true);
		ActionButton showAll = new ActionButton(new CommonButtonData(text(language, "dialog.player_titles.show_all"), Optional.empty(), 100),
			Optional.of(new CustomAll(SEARCH_ACTION, Optional.of(all))));
		Dialogs.open(player, new MultiActionDialog(common, List.of(show, showAll), Optional.empty(), 2));
	}

	/** The search dialog's buttons (registered in PlayerTitles.init). */
	public static final net.minecraft.resources.Identifier SEARCH_ACTION = PlayerTitles.id("search");

	public static void onSearch(ServerPlayer player, CompoundTag inputs) {
		Sort sort = view(player).sort();
		String chosen = inputs.getStringOr("sort", "");
		for (Sort candidate : Sort.values()) {
			if (candidate.name().equalsIgnoreCase(chosen)) {
				sort = candidate;
			}
		}
		boolean all = inputs.getBooleanOr("all", false);
		String search = all ? "" : inputs.getStringOr("search", "").trim();
		String category = all ? ALL : inputs.getStringOr("category", view(player).category());
		VIEWS.put(player.getUUID(), new View(sort, search, category));
		open(player, 0);
	}

	public static void forget(ServerPlayer player) {
		VIEWS.remove(player.getUUID());
	}

	private static int rarity(Title title) {
		int index = RARITIES.indexOf(title.rarity() == null ? "common" : title.rarity().toLowerCase(Locale.ROOT));
		return index < 0 ? RARITIES.size() : index;
	}

	private static Locale locale(String language) {
		return language.startsWith("de") ? Locale.GERMANY : Locale.UK;
	}

	private static String date(long time, String language) {
		Locale locale = locale(language);
		return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
			.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()));
	}

	/** A tooltip text in lines no wider than about the screen (TextWidth.TOOLTIP), so tooltips cover little. */
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
