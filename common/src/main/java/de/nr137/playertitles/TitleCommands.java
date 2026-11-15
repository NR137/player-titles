package de.nr137.playertitles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import de.nr137.lib.lang.Translations;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.display.TitleDisplays;
import de.nr137.playertitles.screen.TitlesScreen;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleText;

/**
 * /titles: the title screen; {@code search [text]} (the screen with matching titles), {@code wear <title>}, {@code off}
 * and {@code animations on|off} (permission
 * player_titles.use). For admins
 * (player_titles.admin): {@code give|take <players> <title>}, {@code list <player>}, {@code reload}, {@code config}
 * (the options; {@code config <option> <value>} changes one right away and saves it).
 */
final class TitleCommands {
	private static final SuggestionProvider<CommandSourceStack> OWNED = (context, builder) -> {
		ServerPlayer player = context.getSource().getPlayer();
		TitleData data = TitleData.get(context.getSource().getServer());
		return SharedSuggestionProvider.suggest(player == null ? java.util.List.of() : data.of(player.getUUID()).owned().keySet(), builder);
	};
	private static final SuggestionProvider<CommandSourceStack> ALL = (context, builder) ->
		SharedSuggestionProvider.suggest(PlayerTitles.titles().all().stream().map(Title::id), builder);

	private TitleCommands() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (PlayerTitles.isDevServer()) {
			registerDev(dispatcher);
		}
		dispatcher.register(Commands.literal("titles")
			.requires(source -> canUse(source) || isAdmin(source))
			.executes(context -> {
				TitlesScreen.open(context.getSource().getPlayerOrException(), 0);
				return 1;
			})
			.then(Commands.literal("wear").requires(TitleCommands::canUse)
				.then(Commands.argument("title", StringArgumentType.word()).suggests(OWNED).executes(TitleCommands::wear)))
			.then(Commands.literal("off").requires(TitleCommands::canUse).executes(TitleCommands::off))
			.then(formCommand())
			.then(Commands.literal("search").requires(TitleCommands::canUse)
				.executes(context -> {
					TitlesScreen.search(context.getSource().getPlayerOrException(), "");
					return 1;
				})
				.then(Commands.argument("text", StringArgumentType.greedyString()).executes(context -> {
					TitlesScreen.search(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "text"));
					return 1;
				})))
			.then(Commands.literal("animations").requires(TitleCommands::canUse)
				.then(Commands.literal("on").executes(context -> animations(context, true)))
				.then(Commands.literal("off").executes(context -> animations(context, false))))
			.then(Commands.literal("give").requires(TitleCommands::isAdmin)
				.then(Commands.argument("players", EntityArgument.players())
					.then(Commands.argument("title", StringArgumentType.word()).suggests(ALL).executes(context -> giveOrTake(context, true)))))
			.then(Commands.literal("take").requires(TitleCommands::isAdmin)
				.then(Commands.argument("players", EntityArgument.players())
					.then(Commands.argument("title", StringArgumentType.word()).suggests(ALL).executes(context -> giveOrTake(context, false)))))
			.then(Commands.literal("list").requires(TitleCommands::isAdmin)
				.then(Commands.argument("player", EntityArgument.player()).executes(TitleCommands::list)))
			.then(Commands.literal("reload").requires(TitleCommands::isAdmin).executes(TitleCommands::reload))
			.then(configCommand().requires(TitleCommands::isAdmin))
			.then(Commands.literal("first").requires(TitleCommands::isAdmin)
				.then(Commands.literal("reset").executes(context -> resetFirsts(context, null))
					.then(Commands.argument("advancement", net.minecraft.commands.arguments.IdentifierArgument.id())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(
							TitleData.get(context.getSource().getServer()).firsts().keySet(), builder))
						.executes(context -> resetFirsts(context,
							net.minecraft.commands.arguments.IdentifierArgument.getId(context, "advancement").toString()))))));
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> configCommand() {
		var config = Commands.literal("config").executes(TitleCommands::showConfig);
		config.then(bool("aboveHead", Config::withAboveHead)).then(bool("playerList", Config::withPlayerList)).then(bool("chat", Config::withChat))
			.then(bool("playerListAnimations", Config::withPlayerListAnimations))
			.then(Commands.literal("playerListAnimationTicks").then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 200))
				.executes(context -> setConfig(context, c -> c.withPlayerListAnimationTicks(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"))))))
			.then(Commands.literal("chatFormat").then(Commands.argument("format", StringArgumentType.greedyString())
				.executes(context -> setConfig(context, c -> c.withChatFormat(StringArgumentType.getString(context, "format"))))))
			.then(Commands.literal("animationTicks").then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20))
				.executes(context -> setConfig(context, c -> c.withAnimationTicks(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"))))));
		var language = Commands.literal("language");
		language.then(Commands.literal(Config.PER_PLAYER).executes(context -> setConfig(context, c -> c.withLanguage(Config.PER_PLAYER))));
		for (String code : Translations.LANGUAGES) {
			language.then(Commands.literal(code).executes(context -> setConfig(context, c -> c.withLanguage(code))));
		}
		var fallback = Commands.literal("fallbackLanguage");
		for (String code : Translations.LANGUAGES) {
			fallback.then(Commands.literal(code).executes(context -> setConfig(context, c -> c.withFallbackLanguage(code))));
		}
		config.then(fallback);
		var defaultForm = Commands.literal("defaultForm");
		for (String name : Title.FORMS) {
			defaultForm.then(Commands.literal(name).executes(context -> setConfig(context, c -> c.withDefaultForm(name))));
		}
		config.then(defaultForm);
		config.then(merchantConfig());
		var permission = Commands.literal("permission");
		for (String node : java.util.List.of("use", "merchant", "admin")) {
			permission.then(Commands.literal(node)
				.then(Commands.literal("everyone").executes(context -> setConfig(context, c -> c.withPermission(node, true))))
				.then(Commands.literal("operators").executes(context -> setConfig(context, c -> c.withPermission(node, false)))));
		}
		return config.then(language).then(permission);
	}

	/**
	 * Dev servers only (PlayerTitles.isDevServer): /titles dev nextday (merchants act as if it were the next day) and
	 * /titles dev currency <players> [count] (today's currency).
	 */
	private static void registerDev(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("titles").then(Commands.literal("dev").requires(TitleCommands::isAdmin)
			.then(Commands.literal("nextday").executes(context -> {
				de.nr137.playertitles.merchant.TitleMerchants.devNextDay();
				context.getSource().sendSuccess(() -> Component.literal("The merchants act as if it were the next day (until the server stops)"), true);
				return 1;
			}))
			.then(Commands.literal("sprint").then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 240000))
				.executes(context -> {
					// /tick needs operator level 3, which command blocks don't have (dev servers only).
					int ticks = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks");
					context.getSource().getServer().tickRateManager().requestGameToSprint(ticks);
					return ticks;
				})))
			.then(Commands.literal("breedtest").executes(context -> breedTest(context, 1000))
				.then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100000))
					.executes(context -> breedTest(context, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count")))))
			.then(Commands.literal("currency").then(Commands.argument("players", EntityArgument.players())
				.executes(context -> giveCurrency(context, 64))
				.then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 2304))
					.executes(context -> giveCurrency(context, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"))))))));
	}

	/**
	 * /titles dev breedtest [count]: the two nearest adult villagers make a baby that many times, the way breeding
	 * does (Villager.getBreedOffspring, with the mod's nitwit chance), without spawning any; how many were nitwits.
	 */
	private static int breedTest(CommandContext<CommandSourceStack> context, int count) {
		var source = context.getSource();
		var level = source.getLevel();
		var villagers = level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,
			new net.minecraft.world.phys.AABB(source.getPosition(), source.getPosition()).inflate(16), villager -> !villager.isBaby());
		villagers.sort(java.util.Comparator.comparingDouble(villager -> villager.distanceToSqr(source.getPosition())));
		if (villagers.size() < 2) {
			source.sendFailure(Component.literal("Needs two adult villagers within 16 blocks"));
			return 0;
		}
		int nitwits = 0;
		for (int i = 0; i < count; i++) {
			var baby = villagers.get(0).getBreedOffspring(level, villagers.get(1));
			if (baby != null && baby.getVillagerData().profession().is(net.minecraft.world.entity.npc.villager.VillagerProfession.NITWIT)) {
				nitwits++;
			}
		}
		double chance = de.nr137.playertitles.merchant.TitleMerchants.config().breedNitwitChance();
		int result = nitwits;
		Component message = Component.literal(String.format(java.util.Locale.ROOT,
			"%d of %d babies would be nitwits (%.1f %%; the setting is %.1f %%)", result, count, 100.0 * result / count, 100.0 * chance));
		source.sendSuccess(() -> message, false);
		// From a command block, the game shows its answer only to operators: the players near it hear it too.
		if (source.getPlayer() == null) {
			level.players().stream().filter(player -> player.position().distanceToSqr(source.getPosition()) <= 32 * 32)
				.forEach(player -> player.sendSystemMessage(message));
		}
		return nitwits;
	}

	private static int giveCurrency(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
		var currency = de.nr137.playertitles.merchant.TitleMerchants.currency(de.nr137.playertitles.merchant.TitleMerchants.today());
		for (ServerPlayer player : EntityArgument.getPlayers(context, "players")) {
			for (int left = count; left > 0; left -= 64) {
				player.getInventory().add(new net.minecraft.world.item.ItemStack(currency, Math.min(64, left)));
			}
		}
		return count;
	}

	/** /titles config merchant <option> <value>: the Title Merchant's options, right away (no reload). */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> merchantConfig() {
		var merchant = Commands.literal("merchant");
		var villagers = Commands.literal("villagers");
		for (String value : de.nr137.playertitles.merchant.MerchantConfig.VILLAGERS) {
			villagers.then(Commands.literal(value).executes(context -> setConfig(context, c -> c.withMerchant(c.merchant().withVillagers(value)))));
		}
		merchant.then(villagers);
		merchant.then(Commands.literal("breedNitwitChance").then(Commands.argument("chance",
			com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0, 1)).executes(context -> setConfig(context,
			c -> c.withMerchant(c.merchant().withBreedNitwitChance(com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "chance")))))));
		merchant.then(Commands.literal("nitwitKillsAffectReputation").then(Commands.argument("value",
			com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(context -> setConfig(context,
			c -> c.withMerchant(c.merchant().withNitwitKillsAffectReputation(com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value")))))));
		merchant.then(Commands.literal("offers").then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
			.executes(context -> setConfig(context, c -> c.withMerchant(c.merchant().withOffers(
				com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count")))))));
		merchant.then(Commands.literal("maxOffers").then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
			.executes(context -> setConfig(context, c -> c.withMerchant(c.merchant().withMaxOffers(
				com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count")))))));
		merchant.then(Commands.literal("reputationPrices").then(Commands.argument("value",
			com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(context -> setConfig(context,
			c -> c.withMerchant(c.merchant().withReputationPrices(com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value")))))));
		merchant.then(Commands.literal("killTitleChance").then(Commands.argument("chance",
			com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0, 1)).executes(context -> setConfig(context,
			c -> c.withMerchant(c.merchant().withKillTitleChance(com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "chance")))))));
		merchant.then(Commands.literal("screen").then(Commands.literal("trading")
				.executes(context -> setConfig(context, c -> c.withMerchant(c.merchant().withScreen("trading")))))
			.then(Commands.literal("chest").executes(context -> setConfig(context, c -> c.withMerchant(c.merchant().withScreen("chest"))))));
		var price = Commands.literal("price");
		for (String rarity : de.nr137.playertitles.merchant.MerchantConfig.RARITIES) {
			price.then(Commands.literal(rarity).then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 2304))
				.executes(context -> setConfig(context, c -> c.withMerchant(c.merchant().withPrice(rarity,
					com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count")))))));
		}
		return merchant.then(price);
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> bool(String name,
			java.util.function.BiFunction<Config, Boolean, Config> change) {
		return Commands.literal(name).then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
			.executes(context -> setConfig(context, c -> change.apply(c, com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value")))));
	}

	private static int setConfig(CommandContext<CommandSourceStack> context, java.util.function.UnaryOperator<Config> change) {
		PlayerTitles.updateConfig(change);
		context.getSource().getServer().getPlayerList().getPlayers().forEach(TitleDisplays::refresh);
		return showConfig(context);
	}

	private static int showConfig(CommandContext<CommandSourceStack> context) {
		Config config = PlayerTitles.config();
		String text = "Player Titles config (saved):"
			+ "\n aboveHead: " + config.aboveHead()
			+ "\n playerList: " + config.playerList()
			+ "\n chat: " + config.chat()
			+ "\n chatFormat: " + config.chatFormat()
			+ "\n language: " + config.language()
			+ "\n fallbackLanguage: " + config.fallbackLanguage()
			+ "\n defaultForm: " + config.defaultForm()
			+ "\n animationTicks: " + config.animationTicks()
			+ "\n playerListAnimations: " + config.playerListAnimations()
			+ "\n playerListAnimationTicks: " + config.playerListAnimationTicks()
			+ "\n merchant villagers: " + config.merchant().villagers()
			+ "\n merchant breedNitwitChance: " + config.merchant().breedNitwitChance()
			+ "\n merchant nitwitKillsAffectReputation: " + config.merchant().nitwitKillsAffectReputation()
			+ "\n merchant offers: " + config.merchant().offers() + " (maxOffers " + config.merchant().maxOffers() + ")"
			+ "\n merchant price: " + config.merchant().prices()
			+ "\n merchant reputationPrices: " + config.merchant().reputationPrices()
			+ "\n merchant killTitleChance: " + config.merchant().killTitleChance()
			+ "\n merchant screen: " + config.merchant().screen()
			+ "\n permission use: " + (config.forEveryone("use") ? "everyone" : "operators")
			+ "\n permission merchant: " + (config.forEveryone("merchant") ? "everyone" : "operators")
			+ "\n permission admin: " + (config.forEveryone("admin") ? "everyone" : "operators");
		context.getSource().sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	/** Sources other than players pass (for /execute as, which checks this with the console's source); they fail later. */
	private static boolean canUse(CommandSourceStack source) {
		return source.getPlayer() == null || PlayerTitles.USE.check(source.getPlayer());
	}

	static boolean isAdmin(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player == null ? source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) : PlayerTitles.ADMIN.check(player);
	}

	private static int wear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String id = StringArgumentType.getString(context, "title");
		if (!TitleActions.wear(player, id)) {
			context.getSource().sendFailure(text(context.getSource(), "command.player_titles.not_owned", id));
			return 0;
		}
		context.getSource().sendSuccess(() -> text(context.getSource(), "command.player_titles.wearing").append(styled(context.getSource(), id)), false);
		return 1;
	}

	private static int off(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		TitleActions.takeOff(context.getSource().getPlayerOrException());
		context.getSource().sendSuccess(() -> text(context.getSource(), "command.player_titles.off"), false);
		return 1;
	}

	/** /titles form male|female|neutral: the form of the player's own titles, in languages that have forms. */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> formCommand() {
		var form = Commands.literal("form").requires(TitleCommands::canUse);
		for (String name : Title.FORMS) {
			form.then(Commands.literal(name).executes(context -> {
				TitleActions.setForm(context.getSource().getPlayerOrException(), name);
				context.getSource().sendSuccess(() -> text(context.getSource(), "command.player_titles.form",
					text(context.getSource(), "form.player_titles." + name).getString()), false);
				return 1;
			}));
		}
		return form;
	}

	private static int animations(CommandContext<CommandSourceStack> context, boolean on) throws CommandSyntaxException {
		TitleActions.setAnimations(context.getSource().getPlayerOrException(), on);
		context.getSource().sendSuccess(() -> text(context.getSource(), on ? "command.player_titles.animations.on" : "command.player_titles.animations.off"), false);
		return 1;
	}

	private static int giveOrTake(CommandContext<CommandSourceStack> context, boolean give) throws CommandSyntaxException {
		String id = StringArgumentType.getString(context, "title");
		Optional<Title> title = PlayerTitles.titles().get(id);
		if (give && title.isEmpty()) {
			context.getSource().sendFailure(text(context.getSource(), "command.player_titles.unknown", id));
			return 0;
		}
		Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
		TitleData data = TitleData.get(context.getSource().getServer());
		int changed = 0;
		for (ServerPlayer player : players) {
			if (give ? data.give(player.getUUID(), id, "admin") : data.take(player.getUUID(), id)) {
				changed++;
				if (give) {
					player.sendSystemMessage(PlayerTitles.translations().component(PlayerTitles.uiLanguage(player), "message.player_titles.got").append(styled(player, id)));
				} else {
					TitleDisplays.refresh(player);
				}
			}
		}
		int count = changed;
		context.getSource().sendSuccess(() -> text(context.getSource(), give ? "command.player_titles.given" : "command.player_titles.taken",
			id, count), true);
		return count;
	}

	private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		TitleData.PlayerRecord record = TitleData.get(context.getSource().getServer()).of(player.getUUID());
		var message = text(context.getSource(), "command.player_titles.list", player.getName().getString(), record.owned().size());
		for (String id : record.owned().keySet()) {
			message.append("\n ").append(Component.literal(id + (record.worn().filter(id::equals).isPresent() ? " * " : " "))).append(styled(context.getSource(), id));
		}
		context.getSource().sendSuccess(() -> message, false);
		return record.owned().size();
	}

	/**
	 * /titles first reset [advancement]: one advancement's "first on the server" title, or every one, is free again,
	 * taken from its holder (after a test phase, or to start over). The next player to complete the advancement gets
	 * it; the start's backfill gives it again from the advancement files.
	 */
	private static int resetFirsts(CommandContext<CommandSourceStack> context, @org.jspecify.annotations.Nullable String advancement) {
		var server = context.getSource().getServer();
		TitleData data = TitleData.get(server);
		int count = 0;
		for (Map.Entry<String, UUID> first : data.firsts().entrySet()) {
			if (advancement != null && !advancement.equals(first.getKey())) {
				continue;
			}
			Title title = PlayerTitles.titles().firstAdvancementTitles().get(first.getKey());
			if (title != null) {
				data.take(first.getValue(), title.id());
			}
			data.releaseFirst(first.getKey());
			count++;
		}
		server.getPlayerList().getPlayers().forEach(TitleDisplays::refresh);
		int reset = count;
		context.getSource().sendSuccess(() -> text(context.getSource(), "command.player_titles.first_reset", reset), true);
		return reset;
	}

	private static int reload(CommandContext<CommandSourceStack> context) {
		PlayerTitles.reload();
		context.getSource().getServer().getPlayerList().getPlayers().forEach(TitleDisplays::refresh);
		context.getSource().sendSuccess(() -> text(context.getSource(), "command.player_titles.reloaded", PlayerTitles.titles().all().size()), true);
		return 1;
	}

	private static String uiLanguage(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player == null ? PlayerTitles.fallbackLanguage() : PlayerTitles.uiLanguage(player);
	}

	private static String titleLanguage(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player == null ? PlayerTitles.sharedLanguage() : PlayerTitles.titleLanguage(player);
	}

	private static net.minecraft.network.chat.MutableComponent text(CommandSourceStack source, String key, Object... args) {
		return PlayerTitles.translations().component(uiLanguage(source), key, args);
	}

	private static Component styled(CommandSourceStack source, String id) {
		ServerPlayer player = source.getPlayer();
		String language = player == null ? titleLanguage(source) : PlayerTitles.withForm(titleLanguage(source), player);
		return PlayerTitles.titles().get(id).<Component>map(title -> TitleText.of(title, language, 0)).orElse(Component.literal(id));
	}

	private static Component styled(ServerPlayer player, String id) {
		String language = PlayerTitles.withForm(PlayerTitles.titleLanguage(player), player);
		return PlayerTitles.titles().get(id).<Component>map(title -> TitleText.of(title, language, 0)).orElse(Component.literal(id));
	}
}
