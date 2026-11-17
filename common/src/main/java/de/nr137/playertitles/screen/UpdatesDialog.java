package de.nr137.playertitles.screen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;
import de.nr137.lib.config.JsonConfig;
import de.nr137.lib.dialog.Dialogs;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.display.TitleDisplays;
import de.nr137.playertitles.title.ShippedTitles;
import de.nr137.playertitles.title.Title;
import de.nr137.playertitles.title.TitleText;

/**
 * The dialog for one shipped title that both an update and the server changed (/titles updates <title>): both
 * versions rendered as titles, then only the fields that differ, and the buttons to keep the server's or take the new
 * one. After a decision the next open one is shown.
 */
public final class UpdatesDialog {
	public static final Identifier ACTION = PlayerTitles.id("updates");
	private static final int WIDTH = 300;

	private UpdatesDialog() {
	}

	public static boolean open(ServerPlayer player, String id) {
		ShippedTitles.Conflict conflict = ShippedTitles.conflicts().stream().filter(candidate -> candidate.id().equals(id)).findFirst().orElse(null);
		if (conflict == null) {
			return false;
		}
		String language = PlayerTitles.uiLanguage(player);
		int index = ShippedTitles.conflicts().indexOf(conflict) + 1;
		List<DialogBody> body = new ArrayList<>();
		MutableComponent head = Component.literal(conflict.id()).withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" · " + conflict.file().getFileName()).withStyle(ChatFormatting.GRAY));
		if (conflict.disabled()) {
			head.append(" ").append(text(language, "command.player_titles.updates.disabled").withStyle(ChatFormatting.GRAY));
		}
		body.add(new PlainMessage(head, WIDTH));
		body.add(new PlainMessage(text(language, "command.player_titles.updates.server").withStyle(ChatFormatting.GOLD)
			.append("  ").append(rendered(conflict.server(), player)), WIDTH));
		body.add(new PlainMessage(text(language, "command.player_titles.updates.shipped").withStyle(ChatFormatting.GOLD)
			.append("  ").append(rendered(conflict.shipped(), player)), WIDTH));

		Map<String, String> server = fields(conflict.server());
		Map<String, String> shipped = fields(conflict.shipped());
		Set<String> keys = new LinkedHashSet<>(shipped.keySet());
		keys.addAll(server.keySet());
		int same = 0;
		MutableComponent changes = Component.empty();
		for (String key : keys) {
			String ours = server.get(key);
			String theirs = shipped.get(key);
			if (java.util.Objects.equals(ours, theirs)) {
				same++;
				continue;
			}
			if (!changes.getSiblings().isEmpty()) {
				changes.append("\n\n");
			}
			changes.append(Component.literal(key).withStyle(ChatFormatting.AQUA))
				.append("\n").append(text(language, "dialog.player_titles.updates.server_value").withStyle(ChatFormatting.GRAY))
				.append(value(ours, language))
				.append("\n").append(text(language, "dialog.player_titles.updates.shipped_value").withStyle(ChatFormatting.GRAY))
				.append(value(theirs, language));
		}
		body.add(new PlainMessage(changes, WIDTH));
		body.add(new PlainMessage(text(language, "dialog.player_titles.updates.same", same).withStyle(ChatFormatting.GRAY), WIDTH));

		CommonDialogData common = new CommonDialogData(
			text(language, "dialog.player_titles.updates.title", index, ShippedTitles.conflicts().size()), Optional.empty(), true, false,
			DialogAction.CLOSE, body, List.of());
		Dialogs.open(player, new MultiActionDialog(common, List.of(
			button(language, "command.player_titles.updates.keep_button", "dialog.player_titles.updates.keep_tooltip", id, "keep"),
			button(language, "command.player_titles.updates.take_button", "dialog.player_titles.updates.take_tooltip", id, "take")),
			Optional.of(new ActionButton(new CommonButtonData(text(language, "dialog.player_titles.updates.later"), Optional.empty(), 100), Optional.empty())),
			2));
		return true;
	}

	/** The dialog's buttons (registered in PlayerTitles.init); admins only. */
	public static void onChoice(ServerPlayer player, CompoundTag inputs) {
		if (!PlayerTitles.ADMIN.check(player)) {
			return;
		}
		String id = inputs.getStringOr("title", "");
		boolean keep = "keep".equals(inputs.getStringOr("choice", ""));
		String language = PlayerTitles.uiLanguage(player);
		if (!resolve(player.level().getServer(), id, keep)) {
			player.sendSystemMessage(text(language, "command.player_titles.updates.unknown", id).withStyle(ChatFormatting.RED));
			return;
		}
		player.sendSystemMessage(text(language, keep ? "command.player_titles.updates.kept" : "command.player_titles.updates.taken", id));
		ShippedTitles.conflicts().stream().findFirst().ifPresent(next -> open(player, next.id()));
	}

	/** Keeps or takes, then reloads the titles. False if there is no such decision open. */
	public static boolean resolve(MinecraftServer server, String id, boolean keep) {
		boolean done = keep ? ShippedTitles.keep(PlayerTitles.titlesFolder(), id) : ShippedTitles.take(PlayerTitles.titlesFolder(), id);
		if (done) {
			PlayerTitles.reload();
			server.getPlayerList().getPlayers().forEach(TitleDisplays::refresh);
		}
		return done;
	}

	private static ActionButton button(String language, String label, String tooltip, String id, String choice) {
		CompoundTag payload = new CompoundTag();
		payload.putString("title", id);
		payload.putString("choice", choice);
		return new ActionButton(new CommonButtonData(text(language, label), Optional.of(text(language, tooltip)), 140),
			Optional.of(new CustomAll(ACTION, Optional.of(payload))));
	}

	private static Component rendered(JsonObject json, ServerPlayer player) {
		try {
			Title title = JsonConfig.GSON.fromJson(json, Title.class);
			return TitleText.of(title, PlayerTitles.titleLanguage(player), 0);
		} catch (RuntimeException e) {
			return Component.literal("?").withStyle(ChatFormatting.RED);
		}
	}

	/** A title's fields, one level deep ("text.en_us", "style.gradient"), without its id and switches. */
	private static Map<String, String> fields(JsonObject title) {
		Map<String, String> fields = new java.util.LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : title.entrySet()) {
			String key = entry.getKey();
			if (key.equals("id") || key.equals("enabled") || key.equals("keep")) {
				continue;
			}
			if (entry.getValue().isJsonObject()) {
				entry.getValue().getAsJsonObject().entrySet().forEach(inner -> fields.put(key + "." + inner.getKey(), shown(inner.getValue())));
			} else {
				fields.put(key, shown(entry.getValue()));
			}
		}
		return fields;
	}

	private static String shown(JsonElement value) {
		return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : value.toString();
	}

	private static Component value(String value, String language) {
		return value == null ? text(language, "dialog.player_titles.updates.missing").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
			: Component.literal(value).withStyle(ChatFormatting.WHITE);
	}

	private static MutableComponent text(String language, String key, Object... args) {
		return PlayerTitles.translations().component(language, key, args);
	}
}
