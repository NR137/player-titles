package de.nr137.playertitles.gametest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.TitleActions;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.display.TitleDisplays;
import de.nr137.playertitles.screen.TitlesScreen;

/**
 * Game tests on a real server, shared by both loaders: fabric/src/gametest and neoforge/src/gametest register each
 * entry. Run with ./tasks.sh gametest.
 */
public final class GameTests {
	private GameTests() {
	}

	/** Test name -> test, in the order they run. */
	public static Map<String, Consumer<GameTestHelper>> all() {
		Map<String, Consumer<GameTestHelper>> tests = new LinkedHashMap<>();
		tests.put("mod_is_running", GameTests::modIsRunning);
		tests.put("titles_are_owned_and_worn", GameTests::ownedAndWorn);
		tests.put("worn_title_rides_the_player", GameTests::ridesThePlayer);
		tests.put("worn_title_in_chat_and_list", GameTests::chatAndList);
		tests.put("title_screen_wears_titles", GameTests::screen);
		tests.put("advancements_give_titles", GameTests::advancements);
		tests.put("nitwits_become_title_merchants", GameTests::merchant);
		tests.put("loot_name_tags_give_titles", GameTests::titleTags);
		tests.put("nitwits_pick_up_name_tags", GameTests::pickUp);
		return tests;
	}

	private static void modIsRunning(GameTestHelper helper) {
		helper.assertTrue(PlayerTitles.isRunning(), "the mod did not get the server's start event");
		helper.succeed();
	}

	private static void ownedAndWorn(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		TitleData data = TitleData.get(helper.getLevel().getServer());
		helper.assertFalse(TitleActions.wear(player, "example_plain"), "a title the player doesn't own can't be worn");
		helper.assertTrue(data.give(player.getUUID(), "example_plain", "admin"), "giving a title");
		helper.assertFalse(data.give(player.getUUID(), "example_plain", "admin"), "a title is owned once");
		helper.assertTrue(TitleActions.wear(player, "example_plain"), "wearing an owned title");
		helper.assertValueEqual(data.worn(player.getUUID()), "example_plain", "worn title");
		data.take(player.getUUID(), "example_plain");
		helper.assertTrue(data.worn(player.getUUID()) == null, "a title taken away is taken off");
		TitleDisplays.refresh(player);
		helper.assertValueEqual(TitleDisplays.displayId(player), -1, "no display without a title");
		helper.assertTrue(TitleDisplays.ownNameId(player) != -1, "the own name without a title too");
		helper.succeed();
	}

	private static void ridesThePlayer(GameTestHelper helper) {
		ServerPlayer player = wearing(helper, "example_rainbow");
		int display = TitleDisplays.displayId(player);
		helper.assertTrue(display != -1, "a worn title has a display");
		helper.assertTrue(helper.getLevel().getEntity(display) == null, "the display is never added to the world");
		int[] passengers = new ClientboundSetPassengersPacket(player).getPassengers();
		helper.assertTrue(Arrays.stream(passengers).anyMatch(id -> id == display), "the passengers packet lists the display");
		TitleActions.takeOff(player);
		helper.assertValueEqual(TitleDisplays.displayId(player), -1, "taking the title off removes the display");
		helper.succeed();
	}

	private static void chatAndList(GameTestHelper helper) {
		ServerPlayer player = wearing(helper, "example_gradient");
		String name = player.getName().getString();
		helper.assertValueEqual(TitleDisplays.chatTitle(player, "en_us").getString(), "Deep Diver", "title in chat");
		helper.assertValueEqual(player.getTabListDisplayName().getString(), "Deep Diver " + name, "player list name");
		helper.assertValueEqual(TitleDisplays.chatTitle(player, "de_de").getString(), "Tiefseetaucher", "title in chat in German");
		var packet = new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
			java.util.EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), java.util.List.of(player));
		helper.assertValueEqual(TitleDisplays.forViewer(packet, player), packet, "an English viewer gets the packet as it is");
		TitleActions.takeOff(player);
		helper.assertTrue(player.getTabListDisplayName() == null, "vanilla's list name without a title");
		helper.succeed();
	}

	private static void screen(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		TitleData.get(helper.getLevel().getServer()).give(player.getUUID(), "example_plain", "admin");
		TitleData.get(helper.getLevel().getServer()).give(player.getUUID(), "example_pulse", "admin");
		TitlesScreen.open(player, 0);
		var menu = player.containerMenu;
		helper.assertValueEqual(menu.getSlot(0).getItem().getItem(), Items.NAME_TAG, "first title");
		helper.assertValueEqual(menu.getSlot(1).getItem().getItem(), Items.NAME_TAG, "second title");
		int ember = "Ember".equals(menu.getSlot(0).getItem().get(DataComponents.CUSTOM_NAME).getString()) ? 0 : 1;
		menu.clicked(ember, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(TitleData.get(helper.getLevel().getServer()).worn(player.getUUID()), "example_pulse", "clicking wears");
		helper.assertTrue(Boolean.TRUE.equals(menu.getSlot(ember).getItem().get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)), "the worn one glints");
		helper.assertValueEqual(menu.getSlot(49).getItem().getItem(), Items.BARRIER, "take-off button");
		helper.assertTrue(player.containerMenu.getCarried().isEmpty() && player.getInventory().isEmpty(), "nothing is taken out");
		menu.clicked(51, 0, ContainerInput.PICKUP, player);
		helper.assertFalse(TitleData.get(helper.getLevel().getServer()).animations(player.getUUID()), "the clock switches moving titles off");
		menu.clicked(49, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(TitleData.get(helper.getLevel().getServer()).worn(player.getUUID()) == null, "the button takes it off");
		TitlesScreen.search(player, "emb");
		helper.assertValueEqual(player.containerMenu.getSlot(0).getItem().get(DataComponents.CUSTOM_NAME).getString(), "Ember", "search");
		helper.assertTrue(player.containerMenu.getSlot(1).getItem().isEmpty(), "only matching titles");
		var inputs = new net.minecraft.nbt.CompoundTag();
		inputs.putString("search", "wand");
		inputs.putString("sort", "name");
		TitlesScreen.onSearch(player, inputs);
		helper.assertValueEqual(player.containerMenu.getSlot(0).getItem().get(DataComponents.CUSTOM_NAME).getString(), "Wanderer",
			"the search dialog's inputs");
		TitlesScreen.search(player, "");
		player.containerMenu.clicked(47, 0, ContainerInput.PICKUP, player);
		player.containerMenu.clicked(47, 0, ContainerInput.PICKUP, player);
		player.containerMenu.clicked(47, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(player.containerMenu.getSlot(0).getItem().get(DataComponents.CUSTOM_NAME).getString(), "Ember",
			"sorted by name: Ember before Wanderer");
		player.closeContainer();
		helper.succeed();
	}

	private static ServerPlayer wearing(GameTestHelper helper, String title) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		TitleData.get(helper.getLevel().getServer()).give(player.getUUID(), title, "admin");
		helper.assertTrue(TitleActions.wear(player, title), "wearing " + title);
		return player;
	}

	/** Completing gives the title and, to the first, the "first" title; losing takes only the first; it exists once. */
	private static void advancements(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		var advancement = server.getAdvancements().get(net.minecraft.resources.Identifier.parse("minecraft:story/root"));
		helper.assertTrue(advancement != null, "the advancement exists");
		TitleData data = TitleData.get(server);
		// The test world is kept between runs: earlier runs' mock players completed it too.
		data.releaseFirst("minecraft:story/root");
		ServerPlayer first = helper.makeMockServerPlayerInLevel();
		first.getAdvancements().award(advancement, "crafting_table");
		helper.assertTrue(data.owns(first.getUUID(), "adv.story.root"), "completing gives the title");
		helper.assertTrue(data.owns(first.getUUID(), "first.story.root"), "the first one gets the first title");
		first.getAdvancements().revoke(advancement, "crafting_table");
		helper.assertFalse(data.owns(first.getUUID(), "adv.story.root"), "losing the advancement takes its title");
		helper.assertTrue(data.owns(first.getUUID(), "first.story.root"), "the first title stays");
		ServerPlayer second = helper.makeMockServerPlayerInLevel();
		second.getAdvancements().award(advancement, "crafting_table");
		helper.assertTrue(data.owns(second.getUUID(), "adv.story.root"), "the second one gets the title");
		helper.assertFalse(data.owns(second.getUUID(), "first.story.root"), "but not the first title, which has its holder");
		helper.succeed();
	}

	/** A nitwit picks up a name tag and becomes a merchant; a player buys an offer with the day's currency. */
	private static void merchant(GameTestHelper helper) {
		var level = helper.getLevel();
		net.minecraft.world.entity.npc.villager.Villager villager = helper.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER,
			new net.minecraft.core.BlockPos(1, 2, 1));
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(),
			net.minecraft.world.entity.npc.villager.VillagerProfession.NONE));
		var tag = new net.minecraft.world.entity.item.ItemEntity(level, villager.getX(), villager.getY(), villager.getZ(),
			new net.minecraft.world.item.ItemStack(Items.NAME_TAG));
		level.addFreshEntity(tag);
		helper.assertFalse(de.nr137.playertitles.merchant.TitleMerchants.pickUp(level, villager, tag), "unemployed: not by default");
		// With unemployed ones allowed: such a merchant doesn't take a job.
		var unemployed = helper.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER, new net.minecraft.core.BlockPos(2, 2, 2));
		de.nr137.playertitles.PlayerTitles.updateConfig(c -> c.withMerchant(c.merchant().withVillagers("unemployed")));
		var otherTag = new net.minecraft.world.entity.item.ItemEntity(level, unemployed.getX(), unemployed.getY(), unemployed.getZ(),
			new net.minecraft.world.item.ItemStack(Items.NAME_TAG));
		level.addFreshEntity(otherTag);
		helper.assertTrue(de.nr137.playertitles.merchant.TitleMerchants.pickUp(level, unemployed, otherTag), "an unemployed one, when allowed");
		unemployed.setVillagerData(unemployed.getVillagerData().withProfession(level.registryAccess(),
			net.minecraft.world.entity.npc.villager.VillagerProfession.FARMER));
		helper.assertTrue(unemployed.getVillagerData().profession().is(net.minecraft.world.entity.npc.villager.VillagerProfession.NONE),
			"an unemployed merchant takes no job");
		de.nr137.playertitles.PlayerTitles.updateConfig(c -> c.withMerchant(c.merchant().withVillagers("nitwits")));
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(),
			net.minecraft.world.entity.npc.villager.VillagerProfession.NITWIT));
		villager.setCustomName(net.minecraft.network.chat.Component.literal("Bob"));
		helper.assertTrue(de.nr137.playertitles.merchant.TitleMerchants.pickUp(level, villager, tag), "a nitwit takes the name tag");
		helper.assertTrue(de.nr137.playertitles.merchant.TitleMerchants.isMerchant(villager), "and is a merchant");
		helper.assertValueEqual(villager.getMainHandItem().getItem(), Items.NAME_TAG, "holding the name tag");
		helper.assertValueEqual(villager.getCustomName().getString(), "Bob", "a villager with a name keeps it");
		int badge = de.nr137.playertitles.merchant.MerchantBadges.displayId(villager);
		helper.assertTrue(badge != -1, "a title above its head");
		helper.assertTrue(java.util.Arrays.stream(new ClientboundSetPassengersPacket(villager).getPassengers()).anyMatch(id -> id == badge),
			"riding the villager");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var renamed = new net.minecraft.world.item.ItemStack(Items.NAME_TAG);
		renamed.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Alice"));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, renamed);
		player.interactOn(villager, net.minecraft.world.InteractionHand.MAIN_HAND, villager.position());
		helper.assertValueEqual(villager.getCustomName().getString(), "Alice", "a name tag renames a merchant as usual");
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
		var shown = de.nr137.playertitles.merchant.MerchantBadges.equipmentFor(new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(
			villager.getId(), java.util.List.of(com.mojang.datafixers.util.Pair.of(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
			villager.getMainHandItem()))), player);
		helper.assertTrue(shown.getSlots().stream().anyMatch(slot -> slot.getFirst() == net.minecraft.world.entity.EquipmentSlot.MAINHAND
			&& slot.getSecond().is(de.nr137.playertitles.merchant.TitleMerchants.currency(de.nr137.playertitles.merchant.TitleMerchants.today()))),
			"players see it holding today's currency");
		helper.assertValueEqual(villager.getMainHandItem().getItem(), Items.NAME_TAG, "saved, it still holds the name tag");
		long day = de.nr137.playertitles.merchant.TitleMerchants.today();
		var offers = de.nr137.playertitles.merchant.TitleMerchants.offers(player, day);
		helper.assertValueEqual(offers.size(), 3, "three offers at first");
		helper.assertValueEqual(de.nr137.playertitles.merchant.TitleMerchants.offers(player, day), offers, "the same all day");
		var title = offers.get(0);
		helper.assertFalse(de.nr137.playertitles.merchant.TitleMerchants.buy(player, villager, title), "not without the currency");
		var currency = de.nr137.playertitles.merchant.TitleMerchants.currency(day);
		player.getInventory().add(new net.minecraft.world.item.ItemStack(currency, 64));
		helper.assertTrue(de.nr137.playertitles.merchant.TitleMerchants.buy(player, villager, title), "buying");
		helper.assertTrue(TitleData.get(level.getServer()).owns(player.getUUID(), title.id()), "the title is theirs");
		int price = de.nr137.playertitles.merchant.TitleMerchants.basePrice(title, day);
		helper.assertTrue(villager.getPlayerReputation(player) > 0, "buying is a trade: good gossip");
		helper.assertValueEqual(de.nr137.playertitles.merchant.TitleMerchants.carried(player, currency), 64 - price, "paid");
		helper.assertValueEqual(de.nr137.playertitles.merchant.TitleMerchants.offers(player, day), offers, "the bought one stays shown today");
		helper.assertFalse(de.nr137.playertitles.merchant.TitleMerchants.buy(player, villager, title), "once");
		int before = de.nr137.playertitles.merchant.TitleMerchants.price(player, villager, offers.get(1));
		villager.setLastHurtByMob(player);
		helper.assertTrue(de.nr137.playertitles.merchant.TitleMerchants.price(player, villager, offers.get(1)) > before,
			"hitting a merchant makes its prices dearer (it isn't counted as a nitwit)");
		// The trading screen, as a client uses it: choose the offer, the game fills in the payment, shift-click the result.
		ServerPlayer trader = helper.makeMockServerPlayerInLevel();
		trader.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		trader.getInventory().add(new net.minecraft.world.item.ItemStack(currency, 64));
		trader.getInventory().add(new net.minecraft.world.item.ItemStack(currency, 64));
		helper.assertTrue(de.nr137.playertitles.merchant.MerchantTrades.open(trader, villager), "the trading screen opens");
		var menu = (net.minecraft.world.inventory.MerchantMenu) trader.containerMenu;
		var traded = de.nr137.playertitles.merchant.TitleMerchants.offers(trader, day).get(0);
		menu.setSelectionHint(0);
		menu.tryMoveItems(0);
		helper.assertValueEqual(menu.getSlot(2).getItem().getItem(), Items.NAME_TAG, "the result: the title as a name tag");
		menu.clicked(2, 0, ContainerInput.QUICK_MOVE, trader);
		helper.assertTrue(TitleData.get(level.getServer()).owns(trader.getUUID(), traded.id()), "taking it gives the title");
		helper.assertTrue(java.util.stream.IntStream.range(0, trader.getInventory().getContainerSize()).mapToObj(trader.getInventory()::getItem).noneMatch(de.nr137.playertitles.merchant.MerchantTrades::isPurchase)
			&& !de.nr137.playertitles.merchant.MerchantTrades.isPurchase(menu.getCarried()), "and no name tag");
		trader.closeContainer();
		helper.assertValueEqual(de.nr137.playertitles.merchant.TitleMerchants.carried(trader, currency),
			128 - de.nr137.playertitles.merchant.TitleMerchants.basePrice(traded, day), "the price paid, the rest given back");
		// Zombie villager and cure: still a merchant, as a villager keeps its job.
		var zombie = villager.convertTo(net.minecraft.world.entity.EntityTypes.ZOMBIE_VILLAGER,
			net.minecraft.world.entity.ConversionParams.single(villager, true, true), mob -> { });
		helper.assertTrue(zombie != null && TitleData.get(level.getServer()).isMerchant(zombie.getUUID()), "the zombie villager stays a merchant");
		villager = zombie.convertTo(net.minecraft.world.entity.EntityTypes.VILLAGER,
			net.minecraft.world.entity.ConversionParams.single(zombie, false, false), mob -> { });
		helper.assertTrue(villager != null && de.nr137.playertitles.merchant.TitleMerchants.isMerchant(villager), "and is one again when cured");
		de.nr137.playertitles.PlayerTitles.updateConfig(c -> c.withMerchant(c.merchant().withKillTitleChance(1.0)));
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		villager.hurtServer(level, level.damageSources().playerAttack(player), 1000);
		helper.assertTrue(TitleData.get(level.getServer()).owns(player.getUUID(), "special.merchant_killer"), "the secret title for its killer");
		helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, villager.getBoundingBox().inflate(3),
			item -> item.getItem().is(Items.NAME_TAG)).isEmpty(), "it drops no name tag");
		de.nr137.playertitles.PlayerTitles.updateConfig(c -> c.withMerchant(c.merchant().withKillTitleChance(0.02)));
		helper.succeed();
	}

	/** A loot chest's name tag becomes a title name tag; using it gives a title and uses it up. */
	private static void titleTags(GameTestHelper helper) {
		var container = new net.minecraft.world.SimpleContainer(3);
		container.setItem(0, new net.minecraft.world.item.ItemStack(Items.NAME_TAG));
		de.nr137.playertitles.loot.TitleTags.onLootFilled(container);
		var stack = container.getItem(0);
		helper.assertTrue(de.nr137.playertitles.loot.TitleTags.isTitleTag(stack), "marked");
		var shown = de.nr137.lib.item.ClientItems.forClient(stack);
		helper.assertTrue(!shown.get(DataComponents.ITEM_NAME).equals(stack.get(DataComponents.ITEM_NAME))
			&& shown.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) && !stack.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE),
			"name and glint only for the client");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // in creative mode items aren't used up
		int before = TitleData.get(helper.getLevel().getServer()).of(player.getUUID()).owned().size();
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		helper.assertTrue(de.nr137.playertitles.loot.TitleTags.use(player, player.getMainHandItem()), "using it");
		helper.assertValueEqual(TitleData.get(helper.getLevel().getServer()).of(player.getUUID()).owned().size(), before + 1, "a new title");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "used up");
		helper.succeed();
	}

	/** With the game's own AI: a nitwit next to a dropped name tag picks it up and becomes a merchant. */
	private static void pickUp(GameTestHelper helper) {
		var level = helper.getLevel();
		var villager = helper.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER, new net.minecraft.core.BlockPos(1, 2, 1));
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(),
			net.minecraft.world.entity.npc.villager.VillagerProfession.NITWIT));
		// At its feet, without the push spawnItem gives (it could roll away while the nitwit walks off).
		var tag = new net.minecraft.world.entity.item.ItemEntity(level, villager.getX(), villager.getY(), villager.getZ(),
			new net.minecraft.world.item.ItemStack(Items.NAME_TAG), 0, 0, 0);
		tag.setNoPickUpDelay();
		level.addFreshEntity(tag);
		helper.succeedWhen(() -> helper.assertTrue(de.nr137.playertitles.merchant.TitleMerchants.isMerchant(villager),
			"the nitwit took the name tag"));
	}
}
