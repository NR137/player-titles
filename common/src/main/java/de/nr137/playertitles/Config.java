package de.nr137.playertitles;

import java.util.LinkedHashMap;
import java.util.Map;
import de.nr137.playertitles.merchant.MerchantConfig;

/**
 * config/player_titles/config.json, read at every server start (missing options keep their defaults).
 *
 * @param aboveHead        show the worn title above the player's name
 * @param playerList       show it before the name in the player list
 * @param chat             show it before the name in chat
 * @param chatFormat       how chat messages look, for every player: the name for the first %s, the message for the
 *                         second; "[%s] %s" by default, vanilla's is "<%s> %s"
 * @param language         the language of titles (above the head, in the player list, in chat and the screen):
 *                         "player" for each player's own (the one their game is set to), or one for everyone ("en_us",
 *                         "de_de"). The screen and messages are always in the player's own language.
 * @param fallbackLanguage for players whose language the mod doesn't have, and for titles without a text in a
 *                         language (then the first language the title has, alphabetically, if not this one either)
 * @param defaultForm      the form of titles for players who haven't chosen one, in languages whose titles have forms
 *                         (German): "male", "female" or "neutral"
 * @param animationTicks   how often animated titles change, in ticks (2 = ten times a second)
 * @param playerListAnimations     animated titles move in the player list too (off: they stand still there; on: every
 *                                 player gets an update of the list for each animated title that often, also with the
 *                                 list closed, since the server can't tell)
 * @param playerListAnimationTicks how often, in ticks (2 = ten times a second, as above the head)
 * @param merchant         the Title Merchant (MerchantConfig)
 * @param permissions      who has each permission without a permission mod: "everyone" or "operators"
 */
public record Config(boolean aboveHead, boolean playerList, boolean chat, String chatFormat, String language,
		String fallbackLanguage, String defaultForm, int animationTicks, boolean playerListAnimations, int playerListAnimationTicks,
		MerchantConfig merchant, Map<String, String> permissions) {
	/** The value of {@code language} for each player's own language. */
	public static final String PER_PLAYER = "player";

	public static Config defaults() {
		Map<String, String> permissions = new LinkedHashMap<>();
		permissions.put("use", "everyone");
		permissions.put("merchant", "everyone");
		permissions.put("admin", "operators");
		return new Config(true, true, true, "[%s] %s", PER_PLAYER, "en_us", "male", 2, false, 2, MerchantConfig.defaults(), permissions);
	}

	/** Options missing from an older or hand-written file, or not valid, get their defaults. */
	public Config completed() {
		Config defaults = defaults();
		Map<String, String> merged = new LinkedHashMap<>(defaults.permissions());
		if (permissions != null) {
			merged.putAll(permissions);
		}
		String format = chatFormat == null || chatFormat.indexOf("%s") == chatFormat.lastIndexOf("%s") ? defaults.chatFormat() : chatFormat;
		return new Config(aboveHead, playerList, chat, format, language == null ? defaults.language() : language,
			fallbackLanguage == null ? defaults.fallbackLanguage() : fallbackLanguage,
			defaultForm == null || !de.nr137.playertitles.title.Title.FORMS.contains(defaultForm) ? defaults.defaultForm() : defaultForm,
			animationTicks < 1 ? defaults.animationTicks() : animationTicks, playerListAnimations,
			playerListAnimationTicks < 1 ? defaults.playerListAnimationTicks() : playerListAnimationTicks,
			merchant == null ? defaults.merchant() : merchant, merged);
	}

	public Config withAboveHead(boolean value) {
		return new Config(value, playerList, chat, chatFormat, language, fallbackLanguage, defaultForm, animationTicks, playerListAnimations, playerListAnimationTicks, merchant, permissions);
	}

	public Config withPlayerList(boolean value) {
		return new Config(aboveHead, value, chat, chatFormat, language, fallbackLanguage, defaultForm, animationTicks, playerListAnimations, playerListAnimationTicks, merchant, permissions);
	}

	public Config withChat(boolean value) {
		return new Config(aboveHead, playerList, value, chatFormat, language, fallbackLanguage, defaultForm, animationTicks, playerListAnimations, playerListAnimationTicks, merchant, permissions);
	}

	public Config withChatFormat(String value) {
		return new Config(aboveHead, playerList, chat, value, language, fallbackLanguage, defaultForm, animationTicks, playerListAnimations, playerListAnimationTicks, merchant, permissions).completed();
	}

	public Config withLanguage(String value) {
		return new Config(aboveHead, playerList, chat, chatFormat, value, fallbackLanguage, defaultForm, animationTicks, playerListAnimations, playerListAnimationTicks, merchant, permissions);
	}

	public Config withFallbackLanguage(String value) {
		return new Config(aboveHead, playerList, chat, chatFormat, language, value, defaultForm, animationTicks, playerListAnimations, playerListAnimationTicks, merchant, permissions);
	}

	public Config withDefaultForm(String value) {
		return new Config(aboveHead, playerList, chat, chatFormat, language, fallbackLanguage, value, animationTicks, playerListAnimations,
			playerListAnimationTicks, merchant, permissions).completed();
	}

	public Config withAnimationTicks(int value) {
		return new Config(aboveHead, playerList, chat, chatFormat, language, fallbackLanguage, defaultForm, value, playerListAnimations, playerListAnimationTicks, merchant, permissions).completed();
	}

	public Config withPlayerListAnimations(boolean value) {
		return new Config(aboveHead, playerList, chat, chatFormat, language, fallbackLanguage, defaultForm, animationTicks, value, playerListAnimationTicks, merchant, permissions);
	}

	public Config withPlayerListAnimationTicks(int value) {
		return new Config(aboveHead, playerList, chat, chatFormat, language, fallbackLanguage, defaultForm, animationTicks, playerListAnimations, value, merchant, permissions)
			.completed();
	}

	public Config withPermission(String permission, boolean forEveryone) {
		Map<String, String> changed = new LinkedHashMap<>(permissions);
		changed.put(permission, forEveryone ? "everyone" : "operators");
		return new Config(aboveHead, playerList, chat, chatFormat, language, fallbackLanguage, defaultForm, animationTicks, playerListAnimations, playerListAnimationTicks, merchant, changed);
	}

	public Config withMerchant(MerchantConfig value) {
		return new Config(aboveHead, playerList, chat, chatFormat, language, fallbackLanguage, defaultForm, animationTicks, playerListAnimations,
			playerListAnimationTicks, value, permissions);
	}

	public boolean perPlayerLanguage() {
		return PER_PLAYER.equals(language);
	}

	public boolean forEveryone(String permission) {
		return !"operators".equalsIgnoreCase(permissions.getOrDefault(permission, "operators"));
	}
}
