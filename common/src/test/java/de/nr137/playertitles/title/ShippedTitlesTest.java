package de.nr137.playertitles.title;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShippedTitlesTest {
	private static final String SET = "special.json";
	private static final String EDITED = "special.merchant_killer";
	private static final String UPDATED = "special.dud";
	private static final String DELETED = "special.all_advancements";
	private static final String ADDED = "special.completionist";

	@Test
	void shippedTitlesArriveAndAdminChangesStay(@TempDir Path folder) throws IOException {
		ShippedTitles.sync(folder);
		for (String set : ShippedTitles.SETS) {
			assertTrue(Files.exists(folder.resolve(set)), set + " written");
		}
		assertTrue(ShippedTitles.conflicts().isEmpty());
		String before = Files.readString(folder.resolve(SET));
		ShippedTitles.sync(folder);
		assertEquals(before, Files.readString(folder.resolve(SET)), "nothing changes without a reason");

		// An admin edits one title, disables one, deletes one; one is new in the next version, and two titles changed
		// in it (simulated through the last-shipped hashes).
		JsonObject set = read(folder);
		title(set, EDITED).getAsJsonObject("text").addProperty("en_us", "Our own text");
		title(set, UPDATED).getAsJsonObject("text").addProperty("en_us", "An old typo");
		title(set, UPDATED).addProperty("enabled", false);
		remove(set, DELETED);
		remove(set, ADDED);
		write(folder, set);
		JsonObject last = JsonParser.parseString(Files.readString(folder.resolve(".last-shipped.json"))).getAsJsonObject();
		last.addProperty(EDITED, "an older shipped version");
		last.addProperty(UPDATED, ShippedTitles.hash(title(set, UPDATED)));
		last.remove(ADDED);
		Files.writeString(folder.resolve(".last-shipped.json"), last.toString());

		ShippedTitles.sync(folder);
		set = read(folder);
		assertNotNull(title(set, ADDED), "a new title is appended");
		assertNull(title(set, DELETED), "a deleted one stays deleted");
		assertEquals("Dud", title(set, UPDATED).getAsJsonObject("text").get("en_us").getAsString(), "unchanged by admins: updated");
		assertFalse(title(set, UPDATED).get("enabled").getAsBoolean(), "and stays disabled");
		assertEquals("Our own text", title(set, EDITED).getAsJsonObject("text").get("en_us").getAsString(), "changed by both: kept");
		assertEquals(List.of(EDITED), ShippedTitles.conflicts().stream().map(ShippedTitles.Conflict::id).toList(), "and to decide");

		assertTrue(ShippedTitles.keep(folder, EDITED));
		ShippedTitles.sync(folder);
		assertTrue(title(read(folder), EDITED).get("keep").getAsBoolean(), "keep is written into the title");
		assertTrue(ShippedTitles.conflicts().isEmpty(), "decided");

		// Without "keep" it stays the server's version, until the mod changes it again.
		set = read(folder);
		title(set, EDITED).remove("keep");
		write(folder, set);
		ShippedTitles.sync(folder);
		assertTrue(ShippedTitles.conflicts().isEmpty());
		last = JsonParser.parseString(Files.readString(folder.resolve(".last-shipped.json"))).getAsJsonObject();
		last.addProperty(EDITED, "an older shipped version");
		Files.writeString(folder.resolve(".last-shipped.json"), last.toString());
		ShippedTitles.sync(folder);
		assertTrue(ShippedTitles.take(folder, EDITED));
		assertEquals("Bad for Business", title(read(folder), EDITED).getAsJsonObject("text").get("en_us").getAsString(), "taken");
	}

	@Test
	void theHashIgnoresSwitchesOrderAndFormatting() {
		JsonObject a = JsonParser.parseString("{\"id\": \"x\", \"text\": {\"en_us\": \"X\", \"de_de\": \"Y\"}, \"weight\": 1.0}").getAsJsonObject();
		JsonObject b = JsonParser.parseString("{\"text\": {\"de_de\": \"Y\", \"en_us\": \"X\"}, \"id\": \"x\", \"weight\": 1, \"enabled\": false, \"keep\": true}").getAsJsonObject();
		assertEquals(ShippedTitles.hash(a), ShippedTitles.hash(b));
		b.getAsJsonObject("text").addProperty("en_us", "Z");
		assertFalse(ShippedTitles.hash(a).equals(ShippedTitles.hash(b)));
	}

	private static JsonObject read(Path folder) throws IOException {
		return JsonParser.parseString(Files.readString(folder.resolve(SET))).getAsJsonObject();
	}

	private static void write(Path folder, JsonObject set) throws IOException {
		Files.writeString(folder.resolve(SET), set.toString());
	}

	private static JsonObject title(JsonObject set, String id) {
		for (var element : set.getAsJsonArray("titles")) {
			if (element.getAsJsonObject().get("id").getAsString().equals(id)) {
				return element.getAsJsonObject();
			}
		}
		return null;
	}

	private static void remove(JsonObject set, String id) {
		JsonArray titles = set.getAsJsonArray("titles");
		titles.remove(title(set, id));
	}
}
