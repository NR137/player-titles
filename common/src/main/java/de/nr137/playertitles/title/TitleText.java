package de.nr137.playertitles.title;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Draws a title as chat text in a language, at a frame of its animation (0 for titles that don't move): every letter
 * gets its own colour; icons and side letters come at the start and the end, in the colours of the first and last
 * letter.
 */
public final class TitleText {
	private static final FontDescription RUNES = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
	private static final FontDescription ILLAGER_RUNES = new FontDescription.Resource(Identifier.withDefaultNamespace("illageralt"));
	/** How many side letters at each end. */
	private static final int SIDE_LETTERS = 2;

	private TitleText() {
	}

	public static MutableComponent of(Title title, String language, long frame) {
		TitleStyle style = title.styleOrPlain();
		Style base = Style.EMPTY.withBold(style.bold()).withItalic(style.italic());
		Style text = base.withObfuscated(style.obfuscated() ? true : null);
		if (style.font() != null && !style.font().isBlank() && !style.font().equals("default")) {
			text = text.withFont(new FontDescription.Resource(Identifier.parse(style.font())));
		}
		int[] codePoints = title.text(language).codePoints().toArray();
		int length = Math.max(1, codePoints.length);
		MutableComponent result = Component.empty();
		int first = colorAt(style, 0, length, frame);
		int last = colorAt(style, length - 1, length, frame);
		List<Component> icons = style.iconList().stream().map(TitleText::icon).toList();
		if (!icons.isEmpty()) {
			icons.forEach(result::append);
			result.append(Component.literal(" "));
		}
		side(result, style, title.id(), base.withColor(first), false);
		for (int i = 0; i < codePoints.length; i++) {
			result.append(Component.literal(new String(Character.toChars(codePoints[i]))).withStyle(text.withColor(colorAt(style, i, length, frame))));
		}
		side(result, style, title.id(), base.withColor(last), true);
		if (!icons.isEmpty()) {
			result.append(Component.literal(" "));
			for (int i = icons.size() - 1; i >= 0; i--) {
				result.append(icons.get(i));
			}
		}
		return result;
	}

	private static int colorAt(TitleStyle style, int i, int length, long frame) {
		List<Integer> gradient = colors(style.gradient());
		int color = parse(style.color(), 0xFFFFFF);
		String animation = style.animated() ? style.animation() : "none";
		return switch (animation) {
			case "rainbow" -> Mth.hsvToRgb((((i * 0.08F) - frame * 0.03F) % 1.0F + 1.0F) % 1.0F, 0.75F, 1.0F) & 0xFFFFFF;
			case "pulse" -> lerp(gradient.isEmpty() ? color : gradient.get(0), 0xFFFFFF, (float) (0.5 - 0.5 * Math.cos(frame * 0.25)) * 0.7F);
			case "wave" -> gradient.size() < 2 ? color : gradientAt(gradient, (float) ((i / (double) length + frame * 0.03) % 1.0), true);
			default -> gradient.size() >= 2 ? gradientAt(gradient, length == 1 ? 0 : i / (float) (length - 1), false) : color;
		};
	}

	private static Component icon(String name) {
		Identifier sprite = Identifier.parse(name);
		Identifier atlas = sprite.getPath().startsWith("item/") ? AtlasIds.ITEMS : AtlasIds.BLOCKS;
		return Component.object(new AtlasSprite(atlas, sprite));
	}

	/** Side letters: "magic" ones keep changing (drawn by the client), runes are fixed per title (from its id). */
	private static void side(MutableComponent result, TitleStyle style, String id, Style color, boolean end) {
		if (style.sides() == null) {
			return;
		}
		Style sideStyle = switch (style.sides()) {
			case "magic" -> color.withObfuscated(true);
			case "runes" -> color.withFont(RUNES);
			case "illager_runes" -> color.withFont(ILLAGER_RUNES);
			default -> null;
		};
		if (sideStyle == null) {
			return;
		}
		StringBuilder letters = new StringBuilder();
		int seed = id.hashCode();
		for (int i = 0; i < SIDE_LETTERS; i++) {
			letters.append((char) ('a' + Math.floorMod(seed >> (i * 5), 26)));
		}
		String side = end ? letters.toString() : letters.reverse().toString();
		if (end) {
			result.append(Component.literal(" ")).append(Component.literal(side).withStyle(sideStyle));
		} else {
			result.append(Component.literal(side).withStyle(sideStyle)).append(Component.literal(" "));
		}
	}

	/** The colour at {@code t} (0 to 1) along the gradient; {@code loop}: back to the first colour at the end. */
	private static int gradientAt(List<Integer> colors, float t, boolean loop) {
		List<Integer> stops = loop ? new ArrayList<>(colors) : colors;
		if (loop) {
			stops.add(colors.get(0));
		}
		float position = t * (stops.size() - 1);
		int index = Math.min((int) position, stops.size() - 2);
		return lerp(stops.get(index), stops.get(index + 1), position - index);
	}

	private static int lerp(int from, int to, float t) {
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (r << 16) | (g << 8) | b;
	}

	private static List<Integer> colors(List<String> hex) {
		return hex == null ? List.of() : hex.stream().map(color -> parse(color, 0xFFFFFF)).toList();
	}

	static int parse(String hex, int fallback) {
		if (hex == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
