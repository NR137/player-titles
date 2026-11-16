package de.nr137.playertitles.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import de.nr137.playertitles.gametest.GameTests;

/** Registers the shared {@link GameTests} with Fabric's game test API, one method per test. */
public class FabricGameTests {
	private static final int MAX_TICKS = 200;

	@GameTest(maxTicks = MAX_TICKS)
	public void modIsRunning(GameTestHelper helper) {
		GameTests.all().get("mod_is_running").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void titlesAreOwnedAndWorn(GameTestHelper helper) {
		GameTests.all().get("titles_are_owned_and_worn").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void wornTitleRidesThePlayer(GameTestHelper helper) {
		GameTests.all().get("worn_title_rides_the_player").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void wornTitleInChatAndList(GameTestHelper helper) {
		GameTests.all().get("worn_title_in_chat_and_list").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void titleScreenWearsTitles(GameTestHelper helper) {
		GameTests.all().get("title_screen_wears_titles").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void advancementsGiveTitles(GameTestHelper helper) {
		GameTests.all().get("advancements_give_titles").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void nitwitsBecomeTitleMerchants(GameTestHelper helper) {
		GameTests.all().get("nitwits_become_title_merchants").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void lootNameTagsGiveTitles(GameTestHelper helper) {
		GameTests.all().get("loot_name_tags_give_titles").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void nitwitsPickUpNameTags(GameTestHelper helper) {
		GameTests.all().get("nitwits_pick_up_name_tags").accept(helper);
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void merchantsKeepTheirNameTag(GameTestHelper helper) {
		GameTests.all().get("merchants_keep_their_name_tag").accept(helper);
	}
}
