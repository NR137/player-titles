package de.nr137.playertitles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleLibrary;
import de.nr137.playertitles.title.TitleStyle;
import de.nr137.playertitles.title.TitleText;

/** Unit tests of the loader-independent code: ./gradlew :common:test (./tasks.sh test-java). */
class PlayerTitlesTest {
	@Test
	void idsUseTheModNamespace() {
		assertEquals("player_titles:thing", PlayerTitles.id("thing").toString());
	}

	@Test
	void theShippedSetsAreWrittenAtTheFirstStart(@TempDir Path dir) {
		TitleLibrary library = TitleLibrary.load(dir.resolve("titles"));
		assertTrue(Files.isRegularFile(dir.resolve("titles/examples.json")));
		assertEquals(13 + 252 + 159 + 5, library.all().size());
		Title rainbow = library.get("example_rainbow").orElseThrow();
		assertEquals("Prismatisch", rainbow.text("de_de"));
		assertEquals("Prismatic", rainbow.text("fr_fr"), "other languages fall back to English");
		assertTrue(rainbow.styleOrPlain().animated());
	}

	@Test
	void setsCanBeSwitchedOffAndIdsCountOnce(@TempDir Path dir) throws IOException {
		Path titles = Files.createDirectories(dir.resolve("titles"));
		Files.writeString(titles.resolve("a.json"), """
			{"titles": [{"id": "one", "text": {"en_us": "First"}}, {"id": "nameless"}]}""");
		Files.writeString(titles.resolve("b.json"), """
			{"titles": [{"id": "one", "text": {"en_us": "Second"}}, {"id": "two", "text": {"en_us": "Two"}}]}""");
		Files.writeString(titles.resolve("c.json"), """
			{"enabled": false, "titles": [{"id": "three", "text": {"en_us": "Three"}}]}""");
		TitleLibrary library = TitleLibrary.load(titles);
		assertEquals("First", library.get("one").orElseThrow().text("en_us"));
		assertTrue(library.get("two").isPresent());
		assertTrue(library.get("three").isEmpty(), "a set switched off");
		assertTrue(library.get("nameless").isEmpty(), "a title without text");
	}

	@Test
	void shippedSetsAreWrittenOnceAndStayRemoved(@TempDir Path dir) throws IOException {
		Path titles = Files.createDirectories(dir.resolve("titles"));
		TitleLibrary.load(titles);
		assertTrue(Files.isRegularFile(titles.resolve("advancements.json")), "a new shipped set reaches an existing folder");
		Files.delete(titles.resolve("examples.json"));
		TitleLibrary library = TitleLibrary.load(titles);
		assertFalse(Files.exists(titles.resolve("examples.json")), "a removed set stays removed");
		assertEquals(126, library.advancementTitles().size());
		assertEquals(126, library.firstAdvancementTitles().size());
		assertEquals(List.of("advancements", "first", "trends", "football", "pop_culture", "roles", "anime", "gaming", "general", "unreadable", "special"), library.categories(), "categories from the titles, in their order");
	}

	@Test
	void aTitlesCategoryIsItsFileUnlessItNamesOne(@TempDir Path dir) {
		TitleLibrary library = TitleLibrary.load(dir.resolve("titles"));
		assertEquals("examples", library.get("example_plain").orElseThrow().category());
		assertEquals("advancements", library.get("adv.end.kill_dragon").orElseThrow().category());
		assertEquals("examples", library.categories().get(2), "a file's titles come in the files' order (advancements, examples, merchant, special)");
		Title first = library.get("first.end.kill_dragon").orElseThrow();
		assertEquals(List.of("item/nether_star", "item/end_crystal"), first.styleOrPlain().iconList(), "the star and the title's own icon");
	}

	@Test
	void gradientsRunFromTheFirstToTheLastColour() {
		Title title = new Title("t", Map.of("en_us", "abc"), null,
			new TitleStyle(null, List.of("#FF0000", "#0000FF"), false, false, null, null, null, null, null, false), null, null, null);
		List<Component> glyphs = TitleText.of(title, "en_us", 0).getSiblings();
		assertEquals(3, glyphs.size());
		assertEquals(0xFF0000, glyphs.get(0).getStyle().getColor().getValue());
		assertEquals(0x0000FF, glyphs.get(2).getStyle().getColor().getValue());
	}

	@Test
	void textsFallBackToTheFallbackLanguageThenTheFirstOne() {
		Title german = new Title("t", Map.of("fr_fr", "Bonjour", "de_de", "Hallo"), null, null, null, null, null);
		assertEquals("Hallo", german.text("de_de"));
		assertEquals("Hallo", german.text("en_us"), "no English: the first language alphabetically (de_de before fr_fr)");
		Title english = new Title("t", Map.of("de_de", "Hallo", "en_us", "Hello"), null, null, null, null, null);
		assertEquals("Hello", english.text("fr_fr"), "the fallback language");
	}

	@Test
	void formsFallBackToTheLanguagesOwnText() {
		Title title = new Title("t", Map.of("en_us", "Dragonslayer", "de_de", "Drachentöter", "de_de/female", "Drachentöterin"), null, null, null, null, null);
		assertEquals("Drachentöterin", title.text("de_de/female"));
		assertEquals("Drachentöter", title.text("de_de/neutral"), "no neutral form: the language's own text");
		assertEquals("Dragonslayer", title.text("en_us/female"), "English has no forms");
		assertTrue(title.hasForms("de_de"));
	}

	@Test
	void missingConfigOptionsGetTheirDefaults() {
		Config config = new Config(true, false, true, "%s says", null, null, "unknown", 0, false, 0, null, null, Map.of("admin", "everyone")).completed();
		assertEquals("player", config.language());
		assertEquals("en_us", config.fallbackLanguage());
		assertEquals("male", config.defaultForm());
		assertEquals(3, config.merchant().offers());
		assertEquals(5, config.merchant().offersAfter(15), "a level more at 5 and at 15 trades");
		assertEquals(7, config.merchant().offersAfter(50), "7 at Master");
		assertTrue(config.forEveryone("merchant"));
		assertEquals(0.85, config.loot().markChance("minecraft:chests/abandoned_mineshaft"));
		assertEquals(0.5, config.loot().markChance("other:chest"), "loot tables not listed");
		assertEquals("[%s] %s", config.chatFormat(), "a format needs the name and the message");
		assertEquals(2, config.animationTicks());
		assertEquals(2, config.playerListAnimationTicks());
		assertTrue(config.forEveryone("use"));
		assertTrue(config.forEveryone("admin"));
	}
}
