package de.nr137.playertitles.title;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import de.nr137.playertitles.PlayerTitles;

/**
 * Brings the mod's own title sets (resources under /defaults/titles/) into a server's titles folder and keeps them up
 * to date without overwriting what the server's admins changed. Per title id, three versions are compared: the one
 * shipped now, the one in the server's files (found in any file there), and the last shipped one this server has seen,
 * kept as a hash in {@code .last-shipped.json} (only ever shipped versions, never the server's own):
 * <ul>
 * <li>not seen before: appended to its set's file (a set's file the server doesn't have yet is written whole);</li>
 * <li>seen, but missing on the server: deleted by an admin, stays deleted;</li>
 * <li>{@code "keep": true} in the server's title: never touched;</li>
 * <li>shipped unchanged since last seen: nothing;</li>
 * <li>shipped changed, the server's title still the last shipped one: overwritten (its {@code enabled} stays);</li>
 * <li>both changed: kept, and listed as a conflict for {@code /titles updates} (keep or take).</li>
 * </ul>
 * {@code enabled} and {@code keep} don't count as changes. Without {@code .last-shipped.json} every title counts as
 * not seen: deleted ones come back and changed ones are conflicts (a reset to the shipped titles).
 */
public final class ShippedTitles {
	/** The sets the mod ships, in /defaults/titles/. */
	static final List<String> SETS = List.of("examples.json", "advancements.json", "merchant-and-loot.json", "special.json");
	private static final String LAST_SHIPPED = ".last-shipped.json";
	private static final Gson GSON = new GsonBuilder().setFormattingStyle(FormattingStyle.PRETTY.withIndent("\t")).disableHtmlEscaping().create();

	/** A title both the mod and the server's admins changed. */
	public record Conflict(String id, Path file, JsonObject server, JsonObject shipped) {
		public boolean disabled() {
			return server.has("enabled") && !server.get("enabled").getAsBoolean();
		}
	}

	private static List<Conflict> conflicts = List.of();

	private ShippedTitles() {
	}

	public static List<Conflict> conflicts() {
		return conflicts;
	}

	/** Compares and updates the server's files (server start, /titles reload, /reload), before they are read. */
	public static void sync(Path folder) {
		try {
			Files.createDirectories(folder);
			Files.deleteIfExists(folder.resolve(".shipped")); // the list of files before the comparison by title
			Map<String, String> last = readLast(folder);
			Map<String, Located> server = index(folder);
			Map<Path, JsonObject> changed = new LinkedHashMap<>();
			List<Conflict> found = new ArrayList<>();
			int appended = 0;
			int updated = 0;
			for (String set : SETS) {
				String raw = resource(set);
				if (raw == null) {
					continue;
				}
				JsonObject shippedSet = JsonParser.parseString(raw).getAsJsonObject();
				Path file = folder.resolve(set);
				List<JsonObject> shippedTitles = titles(shippedSet);
				if (!Files.exists(file) && shippedTitles.stream().noneMatch(title -> last.containsKey(id(title)))) {
					Files.writeString(file, raw); // a new set
					shippedTitles.forEach(title -> last.put(id(title), hash(title)));
					continue;
				}
				for (JsonObject shipped : shippedTitles) {
					String id = id(shipped);
					String shippedHash = hash(shipped);
					String lastHash = last.get(id);
					Located here = server.get(id);
					if (here == null) {
						if (lastHash == null && Files.exists(file)) {
							JsonObject target = changed.computeIfAbsent(file, ShippedTitles::read);
							if (target != null) {
								titlesArray(target).add(shipped.deepCopy());
								last.put(id, shippedHash);
								appended++;
							}
						}
						continue; // deleted by an admin (or its whole file): stays so
					}
					String serverHash = hash(here.title());
					if (isTrue(here.title(), "keep") || serverHash.equals(shippedHash)) {
						last.put(id, shippedHash); // kept by an admin, or the same: compared with this one from now on
						continue;
					}
					if (shippedHash.equals(lastHash)) {
						continue; // only the server's admins changed it
					}
					if (serverHash.equals(lastHash)) {
						JsonObject target = changed.computeIfAbsent(here.file(), ShippedTitles::read);
						if (target != null && replace(target, id, withSwitches(shipped, here.title()))) {
							last.put(id, shippedHash);
							updated++;
						}
						continue;
					}
					found.add(new Conflict(id, here.file(), here.title(), shipped));
				}
			}
			for (Map.Entry<Path, JsonObject> entry : changed.entrySet()) {
				Files.writeString(entry.getKey(), GSON.toJson(entry.getValue()) + "\n");
			}
			writeLast(folder, last);
			conflicts = List.copyOf(found);
			if (appended > 0 || updated > 0) {
				PlayerTitles.LOGGER.info("Shipped titles: {} new ones added, {} updated", appended, updated);
			}
			if (!found.isEmpty()) {
				PlayerTitles.LOGGER.warn("{} shipped titles have changed, and so has this server's version of them: see /titles updates ({})",
					found.size(), String.join(", ", found.stream().map(Conflict::id).toList()));
			}
		} catch (IOException | JsonParseException | IllegalStateException e) {
			PlayerTitles.LOGGER.error("Can't bring the shipped titles to {}: {}", folder, e.toString());
		}
	}

	/** Keeps the server's version of a conflicting title: {@code "keep": true}, compared with the new one from now on. */
	public static boolean keep(Path folder, String id) {
		return resolve(folder, id, (conflict, title) -> {
			title.addProperty("keep", true);
			return title;
		});
	}

	/** Takes the shipped version of a conflicting title (its {@code enabled} stays). */
	public static boolean take(Path folder, String id) {
		return resolve(folder, id, (conflict, title) -> withSwitches(conflict.shipped(), title));
	}

	private static boolean resolve(Path folder, String id, java.util.function.BiFunction<Conflict, JsonObject, JsonObject> change) {
		Conflict conflict = conflicts.stream().filter(candidate -> candidate.id().equals(id)).findFirst().orElse(null);
		if (conflict == null) {
			return false;
		}
		JsonObject set = read(conflict.file());
		JsonObject title = set == null ? null : find(set, id);
		if (title == null) {
			return false;
		}
		try {
			replace(set, id, change.apply(conflict, title));
			Files.writeString(conflict.file(), GSON.toJson(set) + "\n");
			Map<String, String> last = readLast(folder);
			last.put(id, hash(conflict.shipped()));
			writeLast(folder, last);
			return true;
		} catch (IOException e) {
			PlayerTitles.LOGGER.error("Can't write {}: {}", conflict.file(), e.toString());
			return false;
		}
	}

	private record Located(Path file, JsonObject title) {
	}

	/** Every title in the server's files by id (the first one counts, as when loading). */
	private static Map<String, Located> index(Path folder) throws IOException {
		Map<String, Located> found = new LinkedHashMap<>();
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".json") && !path.getFileName().toString().startsWith(".")).sorted().toList()) {
				JsonObject set = read(file);
				if (set != null) {
					for (JsonObject title : titles(set)) {
						if (id(title) != null) {
							found.putIfAbsent(id(title), new Located(file, title));
						}
					}
				}
			}
		}
		return found;
	}

	private static @Nullable JsonObject read(Path file) {
		try {
			JsonElement json = JsonParser.parseString(Files.readString(file));
			return json.isJsonObject() ? json.getAsJsonObject() : null;
		} catch (IOException | JsonParseException e) {
			return null; // TitleLibrary reports it
		}
	}

	private static List<JsonObject> titles(JsonObject set) {
		List<JsonObject> titles = new ArrayList<>();
		if (set.get("titles") instanceof JsonArray array) {
			array.forEach(element -> {
				if (element.isJsonObject()) {
					titles.add(element.getAsJsonObject());
				}
			});
		}
		return titles;
	}

	private static JsonArray titlesArray(JsonObject set) {
		if (!(set.get("titles") instanceof JsonArray)) {
			set.add("titles", new JsonArray());
		}
		return set.getAsJsonArray("titles");
	}

	private static @Nullable JsonObject find(JsonObject set, String id) {
		return titles(set).stream().filter(title -> id.equals(id(title))).findFirst().orElse(null);
	}

	private static boolean replace(JsonObject set, String id, JsonObject title) {
		JsonArray array = titlesArray(set);
		for (int i = 0; i < array.size(); i++) {
			if (array.get(i).isJsonObject() && id.equals(id(array.get(i).getAsJsonObject()))) {
				array.set(i, title);
				return true;
			}
		}
		return false;
	}

	/** The shipped title with the server title's {@code enabled} and {@code keep}. */
	private static JsonObject withSwitches(JsonObject shipped, JsonObject server) {
		JsonObject title = shipped.deepCopy();
		for (String key : List.of("enabled", "keep")) {
			if (server.has(key)) {
				title.add(key, server.get(key));
			}
		}
		return title;
	}

	private static @Nullable String id(JsonObject title) {
		return title.get("id") instanceof JsonPrimitive id && id.isString() ? id.getAsString() : null;
	}

	private static boolean isTrue(JsonObject title, String key) {
		return title.get(key) instanceof JsonPrimitive value && value.isBoolean() && value.getAsBoolean();
	}

	/** A hash of a title without {@code enabled} and {@code keep}, the same for any key order and formatting. */
	static String hash(JsonObject title) {
		JsonObject compared = title.deepCopy();
		compared.remove("enabled");
		compared.remove("keep");
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical(compared).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest, 0, 12);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String canonical(JsonElement json) {
		if (json.isJsonObject()) {
			Map<String, String> sorted = new TreeMap<>();
			json.getAsJsonObject().entrySet().forEach(entry -> sorted.put(entry.getKey(), canonical(entry.getValue())));
			StringBuilder out = new StringBuilder("{");
			sorted.forEach((key, value) -> out.append(new JsonPrimitive(key)).append(':').append(value).append(','));
			return out.append('}').toString();
		}
		if (json.isJsonArray()) {
			StringBuilder out = new StringBuilder("[");
			json.getAsJsonArray().forEach(element -> out.append(canonical(element)).append(','));
			return out.append(']').toString();
		}
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			return new java.math.BigDecimal(json.getAsString()).stripTrailingZeros().toPlainString();
		}
		return json.toString();
	}

	private static @Nullable String resource(String set) throws IOException {
		try (InputStream in = ShippedTitles.class.getResourceAsStream("/defaults/titles/" + set)) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static Map<String, String> readLast(Path folder) {
		Path file = folder.resolve(LAST_SHIPPED);
		try {
			if (Files.isRegularFile(file)) {
				Map<String, String> read = GSON.fromJson(Files.readString(file), new TypeToken<LinkedHashMap<String, String>>() { }.getType());
				if (read != null) {
					return read;
				}
			}
		} catch (IOException | JsonParseException e) {
			PlayerTitles.LOGGER.warn("Can't read {}, every shipped title counts as new: {}", file, e.toString());
		}
		return new LinkedHashMap<>();
	}

	private static void writeLast(Path folder, Map<String, String> last) throws IOException {
		Files.writeString(folder.resolve(LAST_SHIPPED), GSON.toJson(new TreeMap<>(last)) + "\n");
	}

}
