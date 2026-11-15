package de.nr137.playertitles.title;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import de.nr137.playertitles.PlayerTitles;

/**
 * One title, as written in a title file (config/player_titles/titles/*.json).
 *
 * @param id          unique among all titles; what players own (keep it when changing the text)
 * @param text        the title per language ("en_us", "de_de"); languages whose words for people are male or female
 *                    can have forms: "de_de" (male), "de_de/female", "de_de/neutral"
 * @param description what it is, or how it was earned, per language
 * @param style       how it looks
 * @param rarity      common, rare, epic or legendary (how often the merchant offers it, what it costs)
 * @param sources     where it can come from: "admin", "merchant", "loot", "advancement:<id>",
 *                    "first_advancement:<id>"
 * @param category    which category of the title screen it is in (default: the file it is in, without ".json"); its
 *                    name comes from the language files ("category.player_titles.<category>") or is the set's name
 */
public record Title(String id, Map<String, String> text, @Nullable Map<String, String> description, @Nullable TitleStyle style,
		@Nullable String rarity, @Nullable List<String> sources, @Nullable String category) {
	public String text(String language) {
		return pick(text, language, id);
	}

	public String description(String language) {
		return description == null ? "" : pick(description, language, "");
	}

	/** The same title in another category. */
	public Title withCategory(String newCategory) {
		return new Title(id, text, description, style, rarity, sources, newCategory);
	}

	public TitleStyle styleOrPlain() {
		return style == null ? TitleStyle.PLAIN : style;
	}

	public List<String> sourcesOrNone() {
		return sources == null ? List.of() : sources;
	}

	/**
	 * The text in the language, else in the fallback language, else in the first language it has (alphabetically).
	 * {@code language} may name a form too ("de_de/female", see {@link #FORMS}): then that form's text if the title has
	 * one, else the language's own (a title with one text for everyone needs no forms).
	 */
	private static String pick(Map<String, String> texts, String language, String fallback) {
		int slash = language.indexOf('/');
		String base = slash < 0 ? language : language.substring(0, slash);
		String form = slash < 0 ? "" : language.substring(slash);
		String fallbackLanguage = PlayerTitles.fallbackLanguage();
		for (String key : List.of(base + form, base, fallbackLanguage + form, fallbackLanguage)) {
			String text = texts.get(key);
			if (text != null) {
				return text;
			}
		}
		return texts.keySet().stream().filter(key -> !key.contains("/")).sorted().findFirst().map(texts::get).orElse(fallback);
	}

	/** The forms a text can have per language: "de_de" is the male one, "de_de/female" and "de_de/neutral" the others. */
	public static final List<String> FORMS = List.of("male", "female", "neutral");

	/** Whether this title has another form in a language. */
	public boolean hasForms(String language) {
		return text.keySet().stream().anyMatch(key -> key.startsWith(language + "/"));
	}
}
