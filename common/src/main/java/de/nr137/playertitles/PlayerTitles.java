package de.nr137.playertitles;

import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.nr137.lib.LibPlatform;
import de.nr137.lib.config.JsonConfig;
import de.nr137.lib.dialog.Dialogs;
import de.nr137.lib.item.ClientItems;
import de.nr137.playertitles.loot.TitleTags;
import de.nr137.playertitles.merchant.MerchantBadges;
import de.nr137.playertitles.merchant.TitleMerchants;
import de.nr137.lib.lang.Translations;
import de.nr137.lib.permission.Permission;
import de.nr137.lib.permission.Permissions;
import de.nr137.playertitles.advancement.AdvancementTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.display.TitleDisplays;
import de.nr137.playertitles.screen.TitlesScreen;
import de.nr137.playertitles.title.TitleLibrary;

/** The loader-independent entry point: each loader's entry point (fabric/, neoforge/) calls these methods. */
public final class PlayerTitles {
	public static final String MOD_ID = "player_titles";
	public static final String MOD_NAME = "Player Titles";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	/** /titles and wearing titles (everyone by default). */
	public static final Permission USE = Permissions.register(MOD_ID, "use", () -> config().forEveryone("use"));
	/** Making and using Title Merchants (everyone by default). */
	public static final Permission MERCHANT = Permissions.register(MOD_ID, "merchant", () -> config().forEveryone("merchant"));
	/** Giving and taking titles, reloading (operators by default). */
	public static final Permission ADMIN = Permissions.register(MOD_ID, "admin", () -> config().forEveryone("admin"));

	private static final Translations TRANSLATIONS = Translations.load(PlayerTitles.class, MOD_ID);

	private static volatile Config config = Config.defaults();
	private static volatile TitleLibrary titles = TitleLibrary.empty();
	private static volatile boolean running;

	private PlayerTitles() {
	}

	/** Called while the mod initialises (the permissions above must be registered by then). */
	public static void init() {
		Dialogs.register(TitlesScreen.SEARCH_ACTION, TitlesScreen::onSearch);
		ClientItems.register(TitleTags::forClient);
		MerchantBadges.init();
		LOGGER.debug("Permissions {} and {}", USE.node(), ADMIN.node());
	}

	/** A dev server of this repo (fabric/ and neoforge/build.gradle set the property): dev commands, all titles. */
	public static boolean isDevServer() {
		return Boolean.getBoolean("player_titles.dev.giveAllTitles");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static Path folder() {
		return LibPlatform.get().configDir().resolve(MOD_ID);
	}

	public static Config config() {
		return config;
	}

	public static TitleLibrary titles() {
		return titles;
	}

	public static Translations translations() {
		return TRANSLATIONS;
	}

	/** The language a player sees titles in: their own, or the one for everyone (config "language"). */
	public static String titleLanguage(ServerPlayer player) {
		return config.perPlayerLanguage() ? uiLanguage(player) : sharedLanguage();
	}

	/** The language of the screen and messages for a player: always their own (or the fallback if the mod hasn't it). */
	public static String uiLanguage(ServerPlayer player) {
		return Translations.supported(player.clientInformation().language(), fallbackLanguage());
	}

	/** The titles' language for everyone, and for the console: the configured one, or the fallback with per-player ones. */
	public static String sharedLanguage() {
		return config.perPlayerLanguage() ? fallbackLanguage() : Translations.supported(config.language(), fallbackLanguage());
	}

	/** A language with the form {@code wearer} chose for their titles ("de_de/female"; see Title.FORMS). */
	public static String withForm(String language, ServerPlayer wearer) {
		String form = TitleData.get(wearer.level().getServer()).form(wearer.getUUID()).orElse(config.defaultForm());
		return "male".equals(form) ? language : language + "/" + form;
	}

	public static String fallbackLanguage() {
		return Translations.supported(config.fallbackLanguage());
	}

	/** Reads the config and the title files again (server start, /titles reload). */
	public static void reload() {
		config = JsonConfig.load(folder().resolve("config.json"), Config.class, Config.defaults()).completed();
		titles = TitleLibrary.load(folder().resolve("titles"));
	}

	public static void updateConfig(UnaryOperator<Config> change) {
		config = change.apply(config);
		JsonConfig.save(folder().resolve("config.json"), config);
	}

	public static void onServerStarting(MinecraftServer server) {
		reload();
	}

	public static void onServerStarted(MinecraftServer server) {
		AdvancementTitles.backfill(server);
		running = true;
		// The smoke test waits for this line (scripts/smoke_test/smoke_test.py).
		LOGGER.info("{} {} is running on Minecraft {}", MOD_NAME, LibPlatform.get().modVersion(MOD_ID),
			LibPlatform.get().modVersion("minecraft"));
	}

	public static void onServerStopping() {
		running = false;
		TitleDisplays.clear();
		MerchantBadges.clear();
	}

	public static boolean isRunning() {
		return running;
	}

	public static void onServerTick(MinecraftServer server) {
		TitleDisplays.onServerTick(server);
		MerchantBadges.onServerTick(server);
	}

	public static void onJoin(ServerPlayer player) {
		if (isDevServer()) {
			// Only the dev servers set this (fabric/ and neoforge/build.gradle), to try titles out. Not those from
			// advancements, merchants, loot and killing merchants: they come from there, to try that out too.
			TitleData data = TitleData.get(player.level().getServer());
			titles().all().stream()
				.filter(title -> title.sourcesOrNone().stream().noneMatch(source -> source.startsWith(TitleLibrary.ADVANCEMENT)
					|| source.startsWith(TitleLibrary.FIRST_ADVANCEMENT) || source.equals(TitleMerchants.SOURCE) || source.equals(TitleTags.SOURCE)
					|| source.equals(TitleMerchants.KILL_SOURCE)))
				.forEach(title -> data.give(player.getUUID(), title.id(), "admin"));
		}
		AdvancementTitles.onJoin(player);
		TitleDisplays.refresh(player);
	}

	public static void onLeave(ServerPlayer player) {
		MerchantBadges.forget(player);
		TitleDisplays.onLeave(player);
		TitlesScreen.forget(player);
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		TitleCommands.register(dispatcher);
	}
}
