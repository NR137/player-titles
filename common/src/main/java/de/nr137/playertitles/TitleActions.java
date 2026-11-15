package de.nr137.playertitles;

import net.minecraft.server.level.ServerPlayer;
import de.nr137.playertitles.data.TitleData;
import de.nr137.playertitles.display.TitleDisplays;

/** What players do with their titles, from the screen or the commands. */
public final class TitleActions {
	private TitleActions() {
	}

	/** Wears a title the player owns (and that exists); true if it worked. */
	public static boolean wear(ServerPlayer player, String id) {
		TitleData data = TitleData.get(player.level().getServer());
		if (!PlayerTitles.USE.check(player) || !data.owns(player.getUUID(), id) || PlayerTitles.titles().get(id).isEmpty()) {
			return false;
		}
		data.wear(player.getUUID(), id);
		TitleDisplays.refresh(player);
		return true;
	}

	public static void takeOff(ServerPlayer player) {
		TitleData.get(player.level().getServer()).wear(player.getUUID(), null);
		TitleDisplays.refresh(player);
	}

	/** The form of the player's own titles (Title.FORMS); everyone sees their title in it. */
	public static void setForm(ServerPlayer player, String form) {
		TitleData.get(player.level().getServer()).setForm(player.getUUID(), form);
		TitleDisplays.refresh(player);
	}

	/** Whether animated titles move for this player (off: they stand still, fewer packets). */
	public static void setAnimations(ServerPlayer player, boolean on) {
		TitleData.get(player.level().getServer()).setAnimations(player.getUUID(), on);
		TitleDisplays.onOptionsChanged(player);
	}
}
