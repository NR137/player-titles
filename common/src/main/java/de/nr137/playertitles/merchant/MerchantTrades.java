package de.nr137.playertitles.merchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;
import de.nr137.lib.text.TextWidth;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleText;

/**
 * A Title Merchant in the game's own trading screen (merchant.screen "trading"): the player's offers of the day as
 * trades "currency → a name tag named after the title". Taking the result gives the title; the name tag never stays
 * (it is marked and removed after every click and when the screen closes). The merchant's level is the player's:
 * Novice, Apprentice after 5 titles bought, Journeyman after 15, Expert after 30, Master after 50 (merchant.levels).
 */
public final class MerchantTrades implements Merchant {
	private static final String MARK = "purchase";
	/** The game's experience at the start of each level (VillagerData). */
	private static final int MAX_LEVEL = 5;

	private final ServerPlayer player;
	private final Villager villager;
	private final List<Title> titles = new ArrayList<>();
	private final MerchantOffers offers = new MerchantOffers();
	private @Nullable Player trading;

	private boolean changed;

	private MerchantTrades(ServerPlayer player, Villager villager) {
		this.player = player;
		this.villager = villager;
		build();
	}

	/** The offers as they are now (after a trade: the bought one sold out, and a level reached gives one more). */
	private void build() {
		titles.clear();
		offers.clear();
		long day = TitleMerchants.today();
		Item currency = TitleMerchants.currency(day);
		List<String> bought = TitleData.get(player.level().getServer()).trades(player.getUUID()).boughtOn(day);
		for (Title title : TitleMerchants.offers(player, day)) {
			int base = TitleMerchants.basePrice(title, day);
			int price = Math.min(128, TitleMerchants.price(player, villager, title));
			MerchantOffer offer;
			if (base <= 64 && price <= 64) {
				// The game shows the change by reputation like its own: the base price crossed out.
				offer = new MerchantOffer(new ItemCost(currency, base), Optional.empty(), result(title), bought.contains(title.id()) ? 1 : 0, 1, 0, 0.0F);
				offer.setSpecialPriceDiff(price - base);
			} else {
				offer = new MerchantOffer(new ItemCost(currency, Math.min(64, price)),
					price > 64 ? Optional.of(new ItemCost(currency, price - 64)) : Optional.empty(), result(title), bought.contains(title.id()) ? 1 : 0,
					1, 0, 0.0F);
			}
			titles.add(title);
			offers.add(offer);
		}
	}

	/** Opens the trading screen; false if there is nothing to offer (then the player is told). */
	public static boolean open(ServerPlayer player, Villager villager) {
		MerchantTrades trades = new MerchantTrades(player, villager);
		if (trades.offers.isEmpty()) {
			player.sendSystemMessage(text(PlayerTitles.uiLanguage(player), "merchant.player_titles.none"));
			return false;
		}
		Component name = villager.getCustomName() != null ? villager.getCustomName() : villager.getName();
		trades.setTradingPlayer(player);
		OptionalInt id = player.openMenu(new SimpleMenuProvider((containerId, inventory, opener) -> new Menu(containerId, inventory, trades),
			text(PlayerTitles.uiLanguage(player), "merchant.player_titles.screen", name.getString())));
		if (id.isPresent()) {
			player.sendMerchantOffers(id.getAsInt(), trades.offers, trades.level(), trades.getVillagerXp(), true, false);
		}
		return id.isPresent();
	}

	private ItemStack result(Title title) {
		String ui = PlayerTitles.uiLanguage(player);
		String language = PlayerTitles.withForm(PlayerTitles.titleLanguage(player), player);
		ItemStack item = new ItemStack(Items.NAME_TAG);
		item.set(DataComponents.CUSTOM_NAME, TitleText.of(title, language, 0).withStyle(style -> style.withItalic(false)));
		List<Component> lore = new ArrayList<>();
		String description = title.description(language);
		if (!description.isEmpty()) {
			lore.addAll(wrapped(Component.literal(description), ChatFormatting.GRAY));
		}
		lore.addAll(wrapped(text(ui, "rarity.player_titles." + (title.rarity() == null ? "common" : title.rarity())), ChatFormatting.DARK_AQUA));
		int percent = (int) Math.round((TitleMerchants.priceFactor(player, villager) - 1.0) * 100);
		if (percent != 0) {
			lore.addAll(wrapped(text(ui, percent > 0 ? "merchant.player_titles.dearer" : "merchant.player_titles.cheaper", Math.abs(percent)),
				percent > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
		}
		lore.addAll(wrapped(text(ui, "merchant.player_titles.tomorrow"), ChatFormatting.DARK_GRAY));
		item.set(DataComponents.LORE, new ItemLore(lore));
		CustomData.update(DataComponents.CUSTOM_DATA, item, tag -> {
			CompoundTag ours = tag.getCompoundOrEmpty(PlayerTitles.MOD_ID);
			ours.putBoolean(MARK, true);
			tag.put(PlayerTitles.MOD_ID, ours);
		});
		return item;
	}

	/** The merchant's level for this player: one more for each of merchant.levels reached, up to Master. */
	private int level() {
		return TitleMerchants.level(TitleData.get(player.level().getServer()).trades(player.getUUID()).count());
	}

	/** After a trade (Menu.clicked): the client gets the offers, level and bar as they are now. */
	private void refresh(MerchantMenu menu) {
		if (!changed) {
			return;
		}
		changed = false;
		build();
		player.sendMerchantOffers(menu.containerId, offers, level(), getVillagerXp(), true, false);
		menu.slotsChanged(player.getInventory());
	}

	/** Experience for the game's bar: how far the player is towards the next level, in the game's units. */
	@Override
	public int getVillagerXp() {
		int trades = TitleData.get(player.level().getServer()).trades(player.getUUID()).count();
		int level = level();
		if (level >= MAX_LEVEL) {
			return VillagerData.getMinXpPerLevel(MAX_LEVEL - 1) + 1000;
		}
		List<Integer> levels = TitleMerchants.config().levels();
		int from = level == 1 ? 0 : levels.get(level - 2);
		int to = levels.get(level - 1);
		int min = VillagerData.getMinXpPerLevel(level);
		int max = VillagerData.getMaxXpPerLevel(level);
		return min + (int) ((max - min) * (double) (trades - from) / Math.max(1, to - from));
	}

	/** A trade was taken (the payment is already consumed by the game): the title. */
	@Override
	public void notifyTrade(MerchantOffer offer) {
		int index = offers.indexOf(offer);
		if (index < 0) {
			return;
		}
		Title title = titles.get(index);
		TitleData data = TitleData.get(player.level().getServer());
		if (data.give(player.getUUID(), title.id(), TitleMerchants.SOURCE)) {
			data.bought(player.getUUID(), title.id(), TitleMerchants.today());
			if (villager.level() instanceof ServerLevel level) {
				level.onReputationEvent(net.minecraft.world.entity.ai.village.ReputationEventType.TRADE, player, villager);
			}
			player.sendSystemMessage(text(PlayerTitles.uiLanguage(player), "message.player_titles.got")
				.append(TitleText.of(title, PlayerTitles.withForm(PlayerTitles.titleLanguage(player), player), 0)));
			changed = true;
		}
		villager.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
	}

	/** The game's trade sound, at the villager (MerchantMenuMixin). */
	public void playTradeSound() {
		villager.playSound(getNotifyTradeSound(), 1.0F, 1.0F);
	}

	@Override
	public void setTradingPlayer(@Nullable Player player) {
		this.trading = player;
	}

	@Override
	public @Nullable Player getTradingPlayer() {
		return trading;
	}

	@Override
	public MerchantOffers getOffers() {
		return offers;
	}

	@Override
	public void overrideOffers(MerchantOffers offers) {
	}

	@Override
	public void notifyTradeUpdated(ItemStack itemStack) {
	}

	@Override
	public void overrideXp(int xp) {
	}

	@Override
	public boolean showProgressBar() {
		return true;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public boolean isClientSide() {
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return villager.isAlive() && player.distanceToSqr(villager) <= 64.0;
	}

	public static boolean isPurchase(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getCompoundOrEmpty(PlayerTitles.MOD_ID).getBooleanOr(MARK, false);
	}

	/** The trading screen as the server runs it: the result never stays with the player, and can't be dropped. */
	private static final class Menu extends MerchantMenu {
		private final MerchantTrades trades;

		Menu(int containerId, Inventory inventory, MerchantTrades trades) {
			super(containerId, inventory, trades);
			this.trades = trades;
		}

		@Override
		public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
			if (slotIndex == 2 && input == ContainerInput.THROW) {
				sendAllDataToRemote();
				return;
			}
			if (slotIndex == 2 && input == ContainerInput.QUICK_MOVE) {
				// The game repeats a shift-click on the result while the payment covers another offer: here one title
				// is bought at a time.
				quickMoveStack(player, slotIndex);
				purge(player);
				trades.refresh(this);
				sendAllDataToRemote();
				return;
			}
			super.clicked(slotIndex, button, input, player);
			purge(player);
			trades.refresh(this);
		}

		@Override
		public void removed(Player player) {
			super.removed(player);
			purge(player);
		}

		private void purge(Player player) {
			boolean changed = false;
			if (isPurchase(getCarried())) {
				setCarried(ItemStack.EMPTY);
				changed = true;
			}
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				if (isPurchase(inventory.getItem(i))) {
					inventory.setItem(i, ItemStack.EMPTY);
					changed = true;
				}
			}
			if (changed) {
				sendAllDataToRemote();
			}
		}
	}

	private static List<Component> wrapped(Component text, ChatFormatting color) {
		return TextWidth.wrap(text.getString(), TextWidth.TOOLTIP).stream()
			.<Component>map(line -> Component.literal(line).withStyle(style -> style.withItalic(false).withColor(color))).toList();
	}

	private static MutableComponent text(String language, String key, Object... args) {
		return PlayerTitles.translations().component(language, key, args);
	}
}
