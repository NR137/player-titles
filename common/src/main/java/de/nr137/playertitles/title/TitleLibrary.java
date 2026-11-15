package de.nr137.playertitles.title;

import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import de.nr137.lib.config.JsonConfig;
import de.nr137.playertitles.PlayerTitles;

/**
 * All titles, from the files in config/player_titles/titles/. The mod's own sets are written there once (also sets a
 * new version brings), so a server can change, replace or remove them; {@code .shipped} lists the ones written, so a
 * removed set stays removed. A server's own set is just another file.
 */
public final class TitleLibrary {
	/** The sets the mod ships (resources under /defaults/titles/). */
	private static final List<String> SHIPPED = List.of("examples.json", "advancements.json", "special.json");
	private static final String SHIPPED_LIST = ".shipped";

	private final Map<String, Title> titles;
	/** A set's file name (without ".json") -> its name, for categories without a name in the language files. */
	private final Map<String, String> setNames;
	/** Advancement id -> its title, and its "first on the server" title (from the titles' sources). */
	private final Map<String, Title> byAdvancement = new LinkedHashMap<>();
	private final Map<String, Title> firstByAdvancement = new LinkedHashMap<>();

	private TitleLibrary(Map<String, Title> titles, Map<String, String> setNames) {
		this.titles = titles;
		this.setNames = setNames;
		for (Title title : titles.values()) {
			for (String source : title.sourcesOrNone()) {
				if (source.startsWith(ADVANCEMENT)) {
					byAdvancement.putIfAbsent(source.substring(ADVANCEMENT.length()), title);
				} else if (source.startsWith(FIRST_ADVANCEMENT)) {
					firstByAdvancement.putIfAbsent(source.substring(FIRST_ADVANCEMENT.length()), title);
				}
			}
		}
	}

	public static final String ADVANCEMENT = "advancement:";
	public static final String FIRST_ADVANCEMENT = "first_advancement:";

	/** Advancement id -> the title for completing it. */
	public Map<String, Title> advancementTitles() {
		return byAdvancement;
	}

	/** Advancement id -> the title for completing it first on the server. */
	public Map<String, Title> firstAdvancementTitles() {
		return firstByAdvancement;
	}

	public static TitleLibrary empty() {
		return new TitleLibrary(Map.of(), Map.of());
	}

	public static TitleLibrary load(Path folder) {
		writeShipped(folder);
		Map<String, Title> titles = new LinkedHashMap<>();
		Map<String, String> setNames = new LinkedHashMap<>();
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".json")).sorted().toList()) {
				try {
					TitleSet set = JsonConfig.GSON.fromJson(Files.readString(file), TitleSet.class);
					if (set == null || Boolean.FALSE.equals(set.enabled()) || set.titles() == null) {
						continue;
					}
					if (set.name() != null) {
						setNames.putIfAbsent(category(file), set.name());
					}
					for (Title title : set.titles()) {
						if (title.id() == null || title.text() == null || title.text().isEmpty()) {
							PlayerTitles.LOGGER.warn("{}: a title without id or text is skipped", file.getFileName());
						} else if (titles.putIfAbsent(title.id(), title.category() == null ? title.withCategory(category(file)) : title) != null) {
							PlayerTitles.LOGGER.warn("{}: the title id {} is used twice; the first one counts", file.getFileName(), title.id());
						}
					}
				} catch (IOException | JsonParseException e) {
					PlayerTitles.LOGGER.error("Can't read the title file {}: {}", file, e.toString());
				}
			}
		} catch (IOException e) {
			PlayerTitles.LOGGER.error("Can't read the titles folder {}: {}", folder, e.toString());
		}
		PlayerTitles.LOGGER.info("Loaded {} titles", titles.size());
		return new TitleLibrary(titles, setNames);
	}

	private static void writeShipped(Path folder) {
		try {
			Files.createDirectories(folder);
			Path list = folder.resolve(SHIPPED_LIST);
			java.util.Set<String> written = new java.util.LinkedHashSet<>(Files.isRegularFile(list) ? Files.readAllLines(list) : List.of());
			for (String name : SHIPPED) {
				if (!written.add(name) || Files.exists(folder.resolve(name))) {
					continue;
				}
				try (InputStream in = TitleLibrary.class.getResourceAsStream("/defaults/titles/" + name)) {
					if (in != null) {
						Files.copy(in, folder.resolve(name));
					}
				}
			}
			Files.write(list, written);
		} catch (IOException e) {
			PlayerTitles.LOGGER.error("Can't write the shipped titles to {}: {}", folder, e.toString());
		}
	}

	public Optional<Title> get(String id) {
		return Optional.ofNullable(titles.get(id));
	}

	private static String category(Path file) {
		String name = file.getFileName().toString();
		return name.substring(0, name.length() - ".json".length());
	}

	/** The categories, in the order their first titles came (files sorted by name, titles in file order). */
	public List<String> categories() {
		return titles.values().stream().map(Title::category).distinct().toList();
	}

	/** A category's name in a language: from the language files, else its set's name, else its id. */
	public String categoryName(String category, String language) {
		String key = "category.player_titles." + category;
		String translated = PlayerTitles.translations().text(language, key);
		return !translated.equals(key) ? translated : setNames.getOrDefault(category, category);
	}

	/** Whether any title has forms in a language (then players choose theirs). */
	public boolean hasForms(String language) {
		return titles.values().stream().anyMatch(title -> title.hasForms(language));
	}

	public Collection<Title> all() {
		return titles.values();
	}
}
