package de.nr137.playertitles.title;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * How a title looks. One of {@code color} (a single colour), {@code gradient} (two or more colours across the text);
 * {@code animation} moves it: "rainbow" (colours running through the text), "pulse" (the colour fading to white and
 * back), "wave" (the gradient running through the text). Colours are "#RRGGBB". Bold is for rare cases: some letters
 * get hard to read (the m).
 *
 * @param icon       a sprite at the start and the end of the title, from the game's textures: "block/..." (block atlas,
 *                   e.g. "block/fire_0"; moving textures move) or "item/..." (item atlas, e.g. "item/nether_star")
 * @param icons      several sprites, like {@code icon}: in this order at the start, the other way round at the end
 *                   (instead of {@code icon})
 * @param sides      letters at the start and the end, in the title's colours: "magic" (letters that keep changing, like
 *                   the enchanting table's), "runes" (the enchanting table's runes), "illager_runes"
 * @param font       the whole title in a font of the game, for the rare title meant to be unreadable: "alt" (the
 *                   enchanting table's runes), "illageralt" (the illagers' runes)
 * @param obfuscated the whole title as letters that keep changing (rare too: it can't be read)
 */
public record TitleStyle(@Nullable String color, @Nullable List<String> gradient, boolean bold, boolean italic,
		@Nullable String animation, @Nullable String icon, @Nullable List<String> icons, @Nullable String sides, @Nullable String font,
		boolean obfuscated) {
	public static final TitleStyle PLAIN = new TitleStyle("#FFFFFF", null, false, false, null, null, null, null, null, false);

	/** The sprites at each end: {@code icons}, else {@code icon}, else none. */
	public List<String> iconList() {
		if (icons != null && !icons.isEmpty()) {
			return icons;
		}
		return icon == null || icon.isBlank() ? List.of() : List.of(icon);
	}

	public boolean animated() {
		return animation != null && !animation.isBlank() && !animation.equals("none");
	}
}
